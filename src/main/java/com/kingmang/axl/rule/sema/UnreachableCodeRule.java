package com.kingmang.axl.rule.sema;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.kingmang.axl.cfg.BasicBlock;
import com.kingmang.axl.cfg.BlockId;
import com.kingmang.axl.cfg.CfgInstruction;
import com.kingmang.axl.cfg.ControlFlowGraph;
import com.kingmang.axl.cfg.ExecutableScope;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.ProblemCollector;
import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.rule.CfgRule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UnreachableCodeRule extends CfgRule {
    @Override
    protected void analyze(
            ExecutableScope scope,
            ControlFlowGraph graph,
            AnalysisContext context,
            ProblemCollector collector
    ) {
        Map<Node, Boolean> sourceReachability = sourceReachability(graph);
        Map<Statement, Reachability> statementReachability = new IdentityHashMap<>();

        sourceReachability.forEach((source, reachable) -> collectStatements(
                source,
                reachable,
                scope,
                statementReachability
        ));

        Set<Statement> candidates = identitySet();
        statementReachability.forEach((statement, reachability) -> {
            if (reachability.hasSource
                    && !reachability.reachable
                    && !(statement instanceof BlockStmt)
                    && !(statement instanceof EmptyStmt)) {
                candidates.add(statement);
            }
        });

        List<Statement> outermost = candidates.stream()
                .filter(statement -> !hasCandidateAncestor(statement, candidates))
                .filter(statement -> statement.getRange().isPresent())
                .sorted(Comparator.comparing(
                        statement -> statement.getRange().orElseThrow().begin
                ))
                .toList();

        for (Fragment fragment : groupAdjacent(outermost)) {
            collector.report(
                    this,
                    Severity.WARNING,
                    "Unreachable code.",
                    context.getSourceFile().getPath().toString(),
                    Optional.of(fragment.range())
            );
        }
    }

    private static boolean hasCandidateAncestor(
            Statement statement,
            Set<Statement> candidates
    ) {
        Node current = statement.getParentNode().orElse(null);
        while (current != null) {
            if (current instanceof Statement ancestor && candidates.contains(ancestor)) {
                return true;
            }
            current = current.getParentNode().orElse(null);
        }
        return false;
    }

    private static Map<Node, Boolean> sourceReachability(ControlFlowGraph graph) {
        Set<BlockId> reachableBlocks = effectivelyReachableBlocks(graph);
        Map<Node, Boolean> result = new IdentityHashMap<>();
        for (BasicBlock block : graph.blocks()) {
            boolean reachable = reachableBlocks.contains(block.id());
            for (CfgInstruction instruction : block.instructions()) {
                if (instruction.kind() == CfgInstruction.Kind.EVALUATE
                        || instruction.kind() == CfgInstruction.Kind.MONITOR_ENTER) {
                    result.merge(instruction.source(), reachable, Boolean::logicalOr);
                }
            }
        }
        return result;
    }

    /*
     * Calls and other expressions do not have implicit exceptional edges yet. Treat catch
     * entries of a reachable try as roots, while still honoring abrupt completion inside them.
     */
    private static Set<BlockId> effectivelyReachableBlocks(ControlFlowGraph graph) {
        Set<BlockId> result = new LinkedHashSet<>(graph.reachableBlocks());
        boolean addedCatchEntry;
        do {
            Set<TryStmt> reachableTries = identitySet();
            for (BlockId blockId : result) {
                graph.successors(blockId).stream()
                        .map(edge -> edge.sourceNode())
                        .filter(TryStmt.class::isInstance)
                        .map(TryStmt.class::cast)
                        .forEach(reachableTries::add);
            }

            ArrayDeque<BlockId> work = new ArrayDeque<>();
            for (BasicBlock block : graph.blocks()) {
                boolean belongsToReachableTry = block.instructions().stream()
                        .filter(instruction -> instruction.kind()
                                == CfgInstruction.Kind.CATCH_PARAMETER)
                        .map(CfgInstruction::source)
                        .map(source -> ancestor(source, TryStmt.class))
                        .flatMap(Optional::stream)
                        .anyMatch(reachableTries::contains);
                if (belongsToReachableTry && result.add(block.id())) {
                    work.addLast(block.id());
                }
            }
            addedCatchEntry = !work.isEmpty();

            while (!work.isEmpty()) {
                BlockId block = work.removeFirst();
                graph.successors(block).stream()
                        .map(edge -> edge.target())
                        .filter(result::add)
                        .forEach(work::addLast);
            }
        } while (addedCatchEntry);
        return result;
    }

    private static <T extends Node> Optional<T> ancestor(Node node, Class<T> type) {
        Node current = node.getParentNode().orElse(null);
        while (current != null) {
            if (type.isInstance(current)) {
                return Optional.of(type.cast(current));
            }
            current = current.getParentNode().orElse(null);
        }
        return Optional.empty();
    }

    private static void collectStatements(
            Node source,
            boolean reachable,
            ExecutableScope scope,
            Map<Statement, Reachability> statements
    ) {
        Node current = source;
        while (true) {
            if (current instanceof Statement statement) {
                statements.computeIfAbsent(statement, ignored -> new Reachability())
                        .include(reachable);
            }
            if (current == scope.body()) {
                return;
            }
            current = current.getParentNode().orElse(null);
            if (current == null || current == scope.declaration()) {
                return;
            }
        }
    }

    private static List<Fragment> groupAdjacent(List<Statement> statements) {
        List<Fragment> fragments = new ArrayList<>();
        for (Statement statement : statements) {
            Range range = statement.getRange().orElseThrow();
            if (!fragments.isEmpty() && isNextSibling(fragments.getLast().last(), statement)) {
                Fragment previous = fragments.removeLast();
                fragments.add(new Fragment(
                        previous.first(),
                        statement,
                        new Range(previous.range().begin, range.end)
                ));
            } else {
                fragments.add(new Fragment(statement, statement, range));
            }
        }
        return fragments;
    }

    private static boolean isNextSibling(Statement first, Statement second) {
        Optional<Node> parent = first.getParentNode();
        if (parent.isEmpty() || second.getParentNode().orElse(null) != parent.get()) {
            return false;
        }
        List<Statement> siblings = parent.get().getChildNodes().stream()
                .filter(Statement.class::isInstance)
                .map(Statement.class::cast)
                .toList();
        int firstIndex = identityIndexOf(siblings, first);
        return firstIndex >= 0
                && firstIndex + 1 < siblings.size()
                && siblings.get(firstIndex + 1) == second;
    }

    private static int identityIndexOf(List<Statement> statements, Statement target) {
        for (int index = 0; index < statements.size(); index++) {
            if (statements.get(index) == target) {
                return index;
            }
        }
        return -1;
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
    }

    @Override
    public String getId() {
        return Constant.UNREACHABLE_CODE_ID;
    }

    @Override
    public String getDescription() {
        return "Reports source statements that cannot be reached from the executable entry";
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }

    private static final class Reachability {
        private boolean hasSource;
        private boolean reachable;

        private void include(boolean sourceReachable) {
            hasSource = true;
            reachable |= sourceReachable;
        }
    }

    private record Fragment(Statement first, Statement last, Range range) { }
}
