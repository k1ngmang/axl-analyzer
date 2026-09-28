package com.kingmang.axl.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CfgBuilder {
    private final Map<BlockId, MutableBlock> blocks = new LinkedHashMap<>();
    private int nextBlockId;
    private BlockId normalExit;
    private BlockId exceptionalExit;

    public ControlFlowGraph build(ExecutableScope executableScope) {
        Objects.requireNonNull(executableScope, "executableScope");
        blocks.clear();
        nextBlockId = 0;
        BlockId entry = newBlock();
        normalExit = newBlock();
        exceptionalExit = newBlock();

        FlowContext context = FlowContext.root();
        Set<BlockId> tails = buildStatement(executableScope.body(), Set.of(entry), context);
        connectAll(tails, normalExit, EdgeKind.NORMAL, null, executableScope.body());

        List<BasicBlock> result = blocks.values().stream().map(MutableBlock::freeze).toList();
        ControlFlowGraph graph = new ControlFlowGraph(
                executableScope,
                entry,
                normalExit,
                exceptionalExit,
                result
        );
        new CfgValidator().validate(graph);
        return graph;
    }

    private Set<BlockId> buildStatement(
            Statement statement,
            Set<BlockId> incoming,
            FlowContext context
    ) {
        if (statement instanceof BlockStmt block) {
            return buildStatements(block.getStatements(), incoming, context);
        }
        if (statement instanceof IfStmt ifStatement) {
            return buildIf(ifStatement, incoming, context);
        }
        if (statement instanceof WhileStmt whileStatement) {
            return buildWhile(whileStatement, incoming, context, null);
        }
        if (statement instanceof DoStmt doStatement) {
            return buildDo(doStatement, incoming, context, null);
        }
        if (statement instanceof ForStmt forStatement) {
            return buildFor(forStatement, incoming, context, null);
        }
        if (statement instanceof ForEachStmt forEachStatement) {
            return buildForEach(forEachStatement, incoming, context, null);
        }
        if (statement instanceof SwitchStmt switchStatement) {
            return buildSwitch(switchStatement, incoming, context, null);
        }
        if (statement instanceof TryStmt tryStatement) {
            return buildTry(tryStatement, incoming, context);
        }
        if (statement instanceof LabeledStmt labeledStatement) {
            return buildLabeled(labeledStatement, incoming, context);
        }
        if (statement instanceof SynchronizedStmt synchronizedStatement) {
            Set<BlockId> lock = appendInstruction(
                    incoming,
                    synchronizedStatement.getExpression(),
                    CfgInstruction.Kind.MONITOR_ENTER
            );
            BlockId after = newBlock();
            FinallyFrame monitorExit = FinallyFrame.synthetic(
                    synchronizedStatement.getExpression(),
                    CfgInstruction.Kind.MONITOR_EXIT,
                    context.finallyFrame()
            );
            FlowContext synchronizedContext = context.withFinally(monitorExit);
            Set<BlockId> bodyTails = buildStatement(
                    synchronizedStatement.getBody(),
                    lock,
                    synchronizedContext
            );
            routeAll(
                    bodyTails,
                    after,
                    EdgeKind.NORMAL,
                    synchronizedStatement,
                    synchronizedContext,
                    context.finallyFrame()
            );
            return Set.of(after);
        }
        if (statement instanceof ReturnStmt returnStatement) {
            Set<BlockId> evaluated = returnStatement.getExpression()
                    .filter(this::isControlExpression)
                    .map(expression -> emitValue(expression, incoming, context))
                    .orElse(incoming);
            BlockId block = only(appendInstruction(evaluated, returnStatement));
            routeAbrupt(block, normalExit, EdgeKind.RETURN, returnStatement, context, null);
            return Set.of();
        }
        if (statement instanceof ThrowStmt throwStatement) {
            Set<BlockId> evaluated = isControlExpression(throwStatement.getExpression())
                    ? emitValue(throwStatement.getExpression(), incoming, context)
                    : incoming;
            BlockId block = only(appendInstruction(evaluated, throwStatement));
            routeThrow(block, throwStatement, context);
            return Set.of();
        }
        if (statement instanceof BreakStmt breakStatement) {
            BlockId block = only(appendInstruction(incoming, breakStatement));
            JumpTarget target = context.breakTarget(
                    breakStatement.getLabel().map(Node::toString).orElse(null)
            );
            routeAbrupt(
                    block,
                    target.block(),
                    EdgeKind.BREAK,
                    breakStatement,
                    context,
                    target.finallyBoundary()
            );
            return Set.of();
        }
        if (statement instanceof ContinueStmt continueStatement) {
            BlockId block = only(appendInstruction(incoming, continueStatement));
            JumpTarget target = context.continueTarget(
                    continueStatement.getLabel().map(Node::toString).orElse(null)
            );
            routeAbrupt(
                    block,
                    target.block(),
                    EdgeKind.CONTINUE,
                    continueStatement,
                    context,
                    target.finallyBoundary()
            );
            return Set.of();
        }
        if (statement instanceof YieldStmt yieldStatement) {
            Set<BlockId> evaluated = isControlExpression(yieldStatement.getExpression())
                    ? emitValue(yieldStatement.getExpression(), incoming, context)
                    : incoming;
            BlockId block = only(appendInstruction(evaluated, yieldStatement));
            JumpTarget target = context.yieldTarget();
            routeAbrupt(
                    block,
                    target.block(),
                    EdgeKind.YIELD,
                    yieldStatement,
                    context,
                    target.finallyBoundary()
            );
            return Set.of();
        }
        if (statement instanceof EmptyStmt) {
            return incoming;
        }
        if (statement instanceof ExpressionStmt expressionStatement) {
            if (isControlExpression(expressionStatement.getExpression())) {
                Set<BlockId> evaluated = emitValue(
                        expressionStatement.getExpression(),
                        incoming,
                        context
                );
                return appendInstruction(evaluated, statement);
            }
            return appendInstruction(incoming, statement);
        }
        if (statement instanceof ExplicitConstructorInvocationStmt
                || statement instanceof LocalClassDeclarationStmt
                || statement instanceof LocalRecordDeclarationStmt
                || statement instanceof UnparsableStmt) {
            return appendInstruction(incoming, statement);
        }
        return appendInstruction(incoming, statement);
    }

    private Set<BlockId> buildStatements(
            Collection<? extends Statement> statements,
            Set<BlockId> incoming,
            FlowContext context
    ) {
        Set<BlockId> tails = incoming;
        for (Statement statement : statements) {
            if (tails.isEmpty()) {
                // keep unreachable source in the graph, but do not connect it to reachable code
                tails = Set.of(newBlock());
            }
            tails = buildStatement(statement, tails, context);
        }
        return tails;
    }

    private Set<BlockId> buildIf(IfStmt statement, Set<BlockId> incoming, FlowContext context) {
        BlockId thenEntry = newBlock();
        BlockId elseEntry = newBlock();
        BlockId after = newBlock();
        emitCondition(statement.getCondition(), incoming, thenEntry, elseEntry);

        Set<BlockId> thenTails = buildStatement(statement.getThenStmt(), Set.of(thenEntry), context);
        Set<BlockId> elseTails = statement.getElseStmt()
                .map(elseStatement -> buildStatement(elseStatement, Set.of(elseEntry), context))
                .orElse(Set.of(elseEntry));
        connectAll(thenTails, after, EdgeKind.NORMAL, null, statement);
        connectAll(elseTails, after, EdgeKind.NORMAL, null, statement);
        return Set.of(after);
    }

    private Set<BlockId> buildWhile(
            WhileStmt statement,
            Set<BlockId> incoming,
            FlowContext context,
            String label
    ) {
        BlockId conditionEntry = newBlock();
        BlockId bodyEntry = newBlock();
        BlockId after = newBlock();
        connectAll(incoming, conditionEntry, EdgeKind.NORMAL, null, statement);
        emitCondition(statement.getCondition(), Set.of(conditionEntry), bodyEntry, after);

        FlowContext bodyContext = context.pushLoop(after, conditionEntry, label);
        Set<BlockId> tails = buildStatement(statement.getBody(), Set.of(bodyEntry), bodyContext);
        connectAll(tails, conditionEntry, EdgeKind.LOOP_BACK, null, statement);
        return Set.of(after);
    }

    private Set<BlockId> buildDo(
            DoStmt statement,
            Set<BlockId> incoming,
            FlowContext context,
            String label
    ) {
        BlockId bodyEntry = newBlock();
        BlockId conditionEntry = newBlock();
        BlockId after = newBlock();
        connectAll(incoming, bodyEntry, EdgeKind.NORMAL, null, statement);
        FlowContext bodyContext = context.pushLoop(after, conditionEntry, label);
        Set<BlockId> tails = buildStatement(statement.getBody(), Set.of(bodyEntry), bodyContext);
        connectAll(tails, conditionEntry, EdgeKind.NORMAL, null, statement);
        emitCondition(statement.getCondition(), Set.of(conditionEntry), bodyEntry, after);
        return Set.of(after);
    }

    private Set<BlockId> buildFor(
            ForStmt statement,
            Set<BlockId> incoming,
            FlowContext context,
            String label
    ) {
        Set<BlockId> initialized = incoming;
        for (Expression expression : statement.getInitialization()) {
            initialized = appendInstruction(initialized, expression);
        }

        BlockId conditionEntry = newBlock();
        BlockId bodyEntry = newBlock();
        BlockId updateEntry = newBlock();
        BlockId after = newBlock();
        connectAll(initialized, conditionEntry, EdgeKind.NORMAL, null, statement);
        statement.getCompare().ifPresentOrElse(
                condition -> emitCondition(condition, Set.of(conditionEntry), bodyEntry, after),
                () -> edge(conditionEntry, bodyEntry, EdgeKind.TRUE, null, statement)
        );

        FlowContext bodyContext = context.pushLoop(after, updateEntry, label);
        Set<BlockId> tails = buildStatement(statement.getBody(), Set.of(bodyEntry), bodyContext);
        connectAll(tails, updateEntry, EdgeKind.NORMAL, null, statement);
        Set<BlockId> updated = Set.of(updateEntry);
        for (Expression expression : statement.getUpdate()) {
            updated = appendInstruction(updated, expression);
        }
        connectAll(updated, conditionEntry, EdgeKind.LOOP_BACK, null, statement);
        return Set.of(after);
    }

    private Set<BlockId> buildForEach(
            ForEachStmt statement,
            Set<BlockId> incoming,
            FlowContext context,
            String label
    ) {
        Set<BlockId> iterable = appendInstruction(incoming, statement.getIterable());
        BlockId condition = newBlock();
        BlockId bodyEntry = newBlock();
        BlockId after = newBlock();
        connectAll(iterable, condition, EdgeKind.NORMAL, null, statement);
        instruction(condition, statement.getVariable());
        edge(condition, bodyEntry, EdgeKind.TRUE, "has next element", statement);
        edge(condition, after, EdgeKind.FALSE, "iteration complete", statement);

        FlowContext bodyContext = context.pushLoop(after, condition, label);
        Set<BlockId> tails = buildStatement(statement.getBody(), Set.of(bodyEntry), bodyContext);
        connectAll(tails, condition, EdgeKind.LOOP_BACK, null, statement);
        return Set.of(after);
    }

    private Set<BlockId> buildSwitch(
            SwitchStmt statement,
            Set<BlockId> incoming,
            FlowContext context,
            String label
    ) {
        BlockId selector = only(appendInstruction(incoming, statement.getSelector()));
        BlockId after = newBlock();
        List<SwitchEntry> entries = statement.getEntries();
        if (entries.isEmpty()) {
            edge(selector, after, EdgeKind.SWITCH_DEFAULT, "default", statement);
            return Set.of(after);
        }

        List<BlockId> starts = entries.stream().map(ignored -> newBlock()).toList();
        boolean hasDefault = false;
        for (int index = 0; index < entries.size(); index++) {
            SwitchEntry entry = entries.get(index);
            BlockId target = starts.get(index);
            if (entry.isDefault() || entry.getLabels().isEmpty()) {
                hasDefault = true;
                edge(selector, target, EdgeKind.SWITCH_DEFAULT, "default", entry);
            } else {
                for (Expression caseLabel : entry.getLabels()) {
                    edge(selector, target, EdgeKind.SWITCH_CASE, caseLabel.toString(), caseLabel);
                }
            }
        }
        if (!hasDefault) {
            edge(selector, after, EdgeKind.SWITCH_DEFAULT, "no match", statement);
        }

        FlowContext switchContext = context.pushSwitch(after, label);
        for (int index = 0; index < entries.size(); index++) {
            SwitchEntry entry = entries.get(index);
            Set<BlockId> tails = buildStatements(entry.getStatements(), Set.of(starts.get(index)), switchContext);
            boolean fallsThrough = entry.getType() == SwitchEntry.Type.STATEMENT_GROUP;
            BlockId target = fallsThrough && index + 1 < starts.size() ? starts.get(index + 1) : after;
            connectAll(tails, target, EdgeKind.NORMAL, null, entry);
        }
        return Set.of(after);
    }

    private Set<BlockId> buildTry(TryStmt statement, Set<BlockId> incoming, FlowContext context) {
        Set<BlockId> resourceTails = incoming;
        for (Expression resource : statement.getResources()) {
            resourceTails = appendInstruction(resourceTails, resource);
        }

        BlockId tryEntry = newBlock();
        BlockId after = newBlock();
        connectAll(resourceTails, tryEntry, EdgeKind.NORMAL, null, statement);

        FinallyFrame userFinally = statement.getFinallyBlock()
                .map(block -> FinallyFrame.block(block, context.finallyFrame()))
                .orElse(context.finallyFrame());
        FinallyFrame protectedCleanup = userFinally;
        for (Expression resource : statement.getResources()) {
            protectedCleanup = FinallyFrame.synthetic(
                    resource,
                    CfgInstruction.Kind.RESOURCE_CLOSE,
                    protectedCleanup
            );
        }
        List<BlockId> catchEntries = statement.getCatchClauses().stream()
                .map(ignored -> newBlock())
                .toList();
        CatchFrame catchFrame = catchEntries.isEmpty()
                ? context.catchFrame()
                : new CatchFrame(catchEntries, context.catchFrame(), userFinally);

        FlowContext tryContext = context.withFinally(protectedCleanup).withCatch(catchFrame);
        Set<BlockId> tryTails = buildStatement(statement.getTryBlock(), Set.of(tryEntry), tryContext);
        routeAll(tryTails, after, EdgeKind.NORMAL, statement, tryContext, context.finallyFrame());

        for (int index = 0; index < statement.getCatchClauses().size(); index++) {
            CatchClause clause = statement.getCatchClauses().get(index);
            BlockId catchEntry = catchEntries.get(index);
            instruction(catchEntry, clause.getParameter(), CfgInstruction.Kind.CATCH_PARAMETER);
            FlowContext catchContext = context.withFinally(userFinally);
            Set<BlockId> catchTails = buildStatement(clause.getBody(), Set.of(catchEntry), catchContext);
            routeAll(catchTails, after, EdgeKind.NORMAL, clause, catchContext, context.finallyFrame());
        }
        return Set.of(after);
    }

    private Set<BlockId> buildLabeled(
            LabeledStmt statement,
            Set<BlockId> incoming,
            FlowContext context
    ) {
        String label = statement.getLabel().asString();
        Statement nested = statement.getStatement();
        if (nested instanceof WhileStmt loop) {
            return buildWhile(loop, incoming, context, label);
        }
        if (nested instanceof DoStmt loop) {
            return buildDo(loop, incoming, context, label);
        }
        if (nested instanceof ForStmt loop) {
            return buildFor(loop, incoming, context, label);
        }
        if (nested instanceof ForEachStmt loop) {
            return buildForEach(loop, incoming, context, label);
        }
        if (nested instanceof SwitchStmt switchStatement) {
            return buildSwitch(switchStatement, incoming, context, label);
        }

        BlockId after = newBlock();
        FlowContext labeled = context.pushBreak(after, label);
        Set<BlockId> tails = buildStatement(nested, incoming, labeled);
        connectAll(tails, after, EdgeKind.NORMAL, null, statement);
        return Set.of(after);
    }

    private void emitCondition(
            Expression expression,
            Set<BlockId> incoming,
            BlockId whenTrue,
            BlockId whenFalse
    ) {
        if (expression instanceof EnclosedExpr enclosed) {
            emitCondition(enclosed.getInner(), incoming, whenTrue, whenFalse);
            return;
        }
        if (expression instanceof UnaryExpr unary
                && unary.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
            emitCondition(unary.getExpression(), incoming, whenFalse, whenTrue);
            return;
        }
        if (expression instanceof BinaryExpr binary
                && binary.getOperator() == BinaryExpr.Operator.AND) {
            BlockId right = newBlock();
            emitCondition(binary.getLeft(), incoming, right, whenFalse);
            emitCondition(binary.getRight(), Set.of(right), whenTrue, whenFalse);
            return;
        }
        if (expression instanceof BinaryExpr binary
                && binary.getOperator() == BinaryExpr.Operator.OR) {
            BlockId right = newBlock();
            emitCondition(binary.getLeft(), incoming, whenTrue, right);
            emitCondition(binary.getRight(), Set.of(right), whenTrue, whenFalse);
            return;
        }
        if (expression instanceof ConditionalExpr conditional) {
            BlockId thenEntry = newBlock();
            BlockId elseEntry = newBlock();
            emitCondition(conditional.getCondition(), incoming, thenEntry, elseEntry);
            emitCondition(conditional.getThenExpr(), Set.of(thenEntry), whenTrue, whenFalse);
            emitCondition(conditional.getElseExpr(), Set.of(elseEntry), whenTrue, whenFalse);
            return;
        }

        BlockId condition = only(appendInstruction(incoming, expression));
        edge(condition, whenTrue, EdgeKind.TRUE, null, expression);
        edge(condition, whenFalse, EdgeKind.FALSE, null, expression);
    }

    private Set<BlockId> emitValue(
            Expression expression,
            Set<BlockId> incoming,
            FlowContext context
    ) {
        if (expression instanceof EnclosedExpr enclosed) {
            return emitValue(enclosed.getInner(), incoming, context);
        }
        if (expression instanceof SwitchExpr switchExpression) {
            return buildSwitchExpression(switchExpression, incoming, context);
        }
        if (expression instanceof ConditionalExpr
                || expression instanceof BinaryExpr binary
                && (binary.getOperator() == BinaryExpr.Operator.AND
                || binary.getOperator() == BinaryExpr.Operator.OR)) {
            BlockId whenTrue = newBlock();
            BlockId whenFalse = newBlock();
            BlockId after = newBlock();
            emitCondition(expression, incoming, whenTrue, whenFalse);
            edge(whenTrue, after, EdgeKind.NORMAL, "value true", expression);
            edge(whenFalse, after, EdgeKind.NORMAL, "value false", expression);
            return Set.of(after);
        }
        return appendInstruction(incoming, expression);
    }

    private Set<BlockId> buildSwitchExpression(
            SwitchExpr expression,
            Set<BlockId> incoming,
            FlowContext context
    ) {
        BlockId selector = only(appendInstruction(incoming, expression.getSelector()));
        BlockId after = newBlock();
        List<SwitchEntry> entries = expression.getEntries();
        if (entries.isEmpty()) {
            edge(selector, exceptionalExit, EdgeKind.THROW, "no matching case", expression);
            return Set.of(after);
        }

        List<BlockId> starts = entries.stream().map(ignored -> newBlock()).toList();
        boolean hasDefault = false;
        for (int index = 0; index < entries.size(); index++) {
            SwitchEntry entry = entries.get(index);
            BlockId target = starts.get(index);
            if (entry.isDefault() || entry.getLabels().isEmpty()) {
                hasDefault = true;
                edge(selector, target, EdgeKind.SWITCH_DEFAULT, "default", entry);
            } else {
                for (Expression caseLabel : entry.getLabels()) {
                    edge(selector, target, EdgeKind.SWITCH_CASE, caseLabel.toString(), caseLabel);
                }
            }
        }
        if (!hasDefault) {
            edge(selector, exceptionalExit, EdgeKind.THROW, "no matching case", expression);
        }

        FlowContext switchContext = context.pushYield(after);
        for (int index = 0; index < entries.size(); index++) {
            SwitchEntry entry = entries.get(index);
            Set<BlockId> tails;
            if (entry.getType() == SwitchEntry.Type.EXPRESSION
                    && entry.getStatements().size() == 1
                    && entry.getStatement(0).isExpressionStmt()) {
                Expression value = entry.getStatement(0).asExpressionStmt().getExpression();
                tails = emitValue(value, Set.of(starts.get(index)), switchContext);
                connectAll(tails, after, EdgeKind.YIELD, null, entry);
            } else {
                tails = buildStatements(entry.getStatements(), Set.of(starts.get(index)), switchContext);
                connectAll(tails, after, EdgeKind.NORMAL, null, entry);
            }
        }
        return Set.of(after);
    }

    private boolean isControlExpression(Expression expression) {
        Expression unwrapped = expression;
        while (unwrapped instanceof EnclosedExpr enclosed) {
            unwrapped = enclosed.getInner();
        }
        return unwrapped instanceof SwitchExpr
                || unwrapped instanceof ConditionalExpr
                || unwrapped instanceof BinaryExpr binary
                && (binary.getOperator() == BinaryExpr.Operator.AND
                || binary.getOperator() == BinaryExpr.Operator.OR);
    }

    private void routeThrow(BlockId from, Node sourceNode, FlowContext context) {
        CatchFrame catches = context.catchFrame();
        if (catches != null && catches.boundary() == context.finallyFrame()) {
            for (BlockId target : catches.targets()) {
                edge(from, target, EdgeKind.EXCEPTION, null, sourceNode);
            }
            routeThrow(from, sourceNode, context.withCatch(catches.parent()));
            return;
        }
        if (context.finallyFrame() != null) {
            FinallyFrame frame = context.finallyFrame();
            BlockId finallyEntry = newBlock();
            edge(from, finallyEntry, EdgeKind.FINALLY, null, sourceNode);
            FlowContext outer = context.withFinally(frame.parent());
            Set<BlockId> tails = buildCleanup(frame, finallyEntry, outer);
            tails.forEach(tail -> routeThrow(tail, sourceNode, outer));
            return;
        }
        if (catches != null) {
            for (BlockId target : catches.targets()) {
                edge(from, target, EdgeKind.EXCEPTION, null, sourceNode);
            }
            routeThrow(from, sourceNode, context.withCatch(catches.parent()));
            return;
        }
        edge(from, exceptionalExit, EdgeKind.THROW, null, sourceNode);
    }

    private void routeAll(
            Set<BlockId> blocks,
            BlockId target,
            EdgeKind kind,
            Node sourceNode,
            FlowContext context,
            FinallyFrame finallyBoundary
    ) {
        blocks.forEach(block -> routeAbrupt(
                block,
                target,
                kind,
                sourceNode,
                context,
                finallyBoundary
        ));
    }

    private void routeAbrupt(
            BlockId from,
            BlockId target,
            EdgeKind kind,
            Node sourceNode,
            FlowContext context,
            FinallyFrame finallyBoundary
    ) {
        FinallyFrame frame = context.finallyFrame();
        if (frame == finallyBoundary) {
            edge(from, target, kind, null, sourceNode);
            return;
        }

        BlockId finallyEntry = newBlock();
        edge(from, finallyEntry, EdgeKind.FINALLY, kind.name(), sourceNode);
        FlowContext outer = context.withFinally(frame.parent());
        Set<BlockId> tails = buildCleanup(frame, finallyEntry, outer);
        tails.forEach(tail -> routeAbrupt(
                tail,
                target,
                kind,
                sourceNode,
                outer,
                finallyBoundary
        ));
    }

    private Set<BlockId> appendInstruction(Set<BlockId> incoming, Node source) {
        return appendInstruction(incoming, source, CfgInstruction.Kind.EVALUATE);
    }

    private Set<BlockId> appendInstruction(
            Set<BlockId> incoming,
            Node source,
            CfgInstruction.Kind kind
    ) {
        if (incoming.size() == 1) {
            BlockId candidate = incoming.iterator().next();
            MutableBlock mutableBlock = blocks.get(candidate);
            if (mutableBlock.edges.isEmpty()) {
                instruction(candidate, source, kind);
                return Set.of(candidate);
            }
        }
        BlockId block = newBlock();
        connectAll(incoming, block, EdgeKind.NORMAL, null, source);
        instruction(block, source, kind);
        return Set.of(block);
    }

    private void instruction(BlockId block, Node source) {
        instruction(block, source, CfgInstruction.Kind.EVALUATE);
    }

    private void instruction(BlockId block, Node source, CfgInstruction.Kind kind) {
        blocks.get(block).instructions.add(new CfgInstruction(source, kind));
    }

    private Set<BlockId> buildCleanup(
            FinallyFrame frame,
            BlockId entry,
            FlowContext outerContext
    ) {
        if (frame.body() != null) {
            return buildStatement(frame.body(), Set.of(entry), outerContext);
        }
        instruction(entry, frame.source(), frame.instructionKind());
        return Set.of(entry);
    }

    private void connectAll(
            Collection<BlockId> sources,
            BlockId target,
            EdgeKind kind,
            String label,
            Node sourceNode
    ) {
        sources.forEach(source -> edge(source, target, kind, label, sourceNode));
    }

    private void edge(
            BlockId source,
            BlockId target,
            EdgeKind kind,
            String label,
            Node sourceNode
    ) {
        blocks.get(source).edges.add(new CfgEdge(source, target, kind, label, sourceNode));
    }

    private BlockId newBlock() {
        BlockId id = new BlockId(nextBlockId++);
        blocks.put(id, new MutableBlock(id));
        return id;
    }

    private static BlockId only(Set<BlockId> blocks) {
        if (blocks.size() != 1) {
            throw new IllegalStateException("Expected one block, got " + blocks.size());
        }
        return blocks.iterator().next();
    }

    private static final class MutableBlock {
        private final BlockId id;
        private final List<CfgInstruction> instructions = new ArrayList<>();
        private final List<CfgEdge> edges = new ArrayList<>();

        private MutableBlock(BlockId id) {
            this.id = id;
        }

        private BasicBlock freeze() {
            return new BasicBlock(id, instructions, edges);
        }
    }

    private record FinallyFrame(
            BlockStmt body,
            Node source,
            CfgInstruction.Kind instructionKind,
            FinallyFrame parent
    ) {
        private static FinallyFrame block(BlockStmt body, FinallyFrame parent) {
            return new FinallyFrame(Objects.requireNonNull(body), null, null, parent);
        }

        private static FinallyFrame synthetic(
                Node source,
                CfgInstruction.Kind instructionKind,
                FinallyFrame parent
        ) {
            return new FinallyFrame(
                    null,
                    Objects.requireNonNull(source),
                    Objects.requireNonNull(instructionKind),
                    parent
            );
        }
    }

    private record CatchFrame(List<BlockId> targets, CatchFrame parent, FinallyFrame boundary) {
        private CatchFrame {
            targets = List.copyOf(targets);
        }
    }

    private record JumpTarget(
            BlockId block,
            String label,
            JumpTarget parent,
            FinallyFrame finallyBoundary
    ) {}

    private record FlowContext(
            JumpTarget breaks,
            JumpTarget continues,
            JumpTarget yields,
            FinallyFrame finallyFrame,
            CatchFrame catchFrame
    ) {
        private static FlowContext root() {
            return new FlowContext(null, null, null, null, null);
        }

        private FlowContext pushLoop(BlockId breakTarget, BlockId continueTarget, String label) {
            return new FlowContext(
                    new JumpTarget(breakTarget, label, breaks, finallyFrame),
                    new JumpTarget(continueTarget, label, continues, finallyFrame),
                    yields,
                    finallyFrame,
                    catchFrame
            );
        }

        private FlowContext pushSwitch(BlockId breakTarget, String label) {
            return new FlowContext(
                    new JumpTarget(breakTarget, label, breaks, finallyFrame),
                    continues,
                    new JumpTarget(breakTarget, label, yields, finallyFrame),
                    finallyFrame,
                    catchFrame
            );
        }

        private FlowContext pushYield(BlockId target) {
            return new FlowContext(
                    breaks,
                    continues,
                    new JumpTarget(target, null, yields, finallyFrame),
                    finallyFrame,
                    catchFrame
            );
        }

        private FlowContext pushBreak(BlockId target, String label) {
            return new FlowContext(
                    new JumpTarget(target, label, breaks, finallyFrame),
                    continues,
                    yields,
                    finallyFrame,
                    catchFrame
            );
        }

        private FlowContext withFinally(FinallyFrame value) {
            return new FlowContext(breaks, continues, yields, value, catchFrame);
        }

        private FlowContext withCatch(CatchFrame value) {
            return new FlowContext(breaks, continues, yields, finallyFrame, value);
        }

        private JumpTarget breakTarget(String label) {
            return find(breaks, label, "break");
        }

        private JumpTarget continueTarget(String label) {
            return find(continues, label, "continue");
        }

        private JumpTarget yieldTarget() {
            return find(yields, null, "yield");
        }

        private static JumpTarget find(JumpTarget target, String label, String kind) {
            for (JumpTarget current = target; current != null; current = current.parent()) {
                if (label == null || label.equals(current.label())) {
                    return current;
                }
            }
            throw new IllegalStateException("No target for " + kind + (label == null ? "" : " " + label));
        }
    }
}
