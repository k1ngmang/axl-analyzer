package com.kingmang.axl.cfg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CfgBuilderTest {
    @Test
    void lowersShortCircuitConditionsIntoSeparateBranches() {
        ControlFlowGraph graph = graph("""
                void example(boolean a, boolean b, boolean c) {
                    if (a && (b || c)) work();
                }
                """);

        BasicBlock a = blockWithName(graph, "a");
        BasicBlock b = blockWithName(graph, "b");
        BasicBlock c = blockWithName(graph, "c");

        assertEquals(2, a.outgoingEdges().size());
        assertEquals(2, b.outgoingEdges().size());
        assertEquals(2, c.outgoingEdges().size());
        assertEquals(b.id(), edge(a, EdgeKind.TRUE).target());
        assertEquals(c.id(), edge(b, EdgeKind.FALSE).target());
        assertNotEquals(c.id(), edge(a, EdgeKind.FALSE).target());
    }

    @Test
    void keepsUnreachableSourceDisconnected() {
        ControlFlowGraph graph = graph("""
                void example() {
                    return;
                    work();
                }
                """);

        BasicBlock work = graph.blocks().stream()
                .filter(block -> block.instructions().stream().anyMatch(instruction ->
                        instruction.source() instanceof ExpressionStmt
                                && instruction.source().toString().equals("work();")))
                .findFirst()
                .orElseThrow();

        assertFalse(graph.reachableBlocks().contains(work.id()));
        assertTrue(graph.reachableBlocks().contains(graph.normalExit()));
    }

    @Test
    void routesReturnThroughFinally() {
        ControlFlowGraph graph = graph("""
                int example() {
                    try {
                        return value();
                    } finally {
                        cleanup();
                    }
                }
                """);

        BasicBlock returnBlock = blockWithInstruction(graph, ReturnStmt.class, "return value();");
        CfgEdge intoFinally = edge(returnBlock, EdgeKind.FINALLY);
        assertFalse(returnBlock.outgoingEdges().stream().anyMatch(edge -> edge.kind() == EdgeKind.RETURN));

        BasicBlock cleanup = graph.blocks().stream()
                .filter(block -> block.instructions().stream().anyMatch(instruction ->
                        instruction.source().toString().equals("cleanup();")))
                .findFirst()
                .orElseThrow();
        assertTrue(graph.reachableBlocks().contains(intoFinally.target()));
        assertEquals(graph.normalExit(), edge(cleanup, EdgeKind.RETURN).target());
    }

    @Test
    void returnInFinallyOverridesPendingReturn() {
        ControlFlowGraph graph = graph("""
                int example() {
                    try {
                        return first();
                    } finally {
                        return second();
                    }
                }
                """);

        BasicBlock first = blockWithInstruction(graph, ReturnStmt.class, "return first();");
        BasicBlock second = blockWithInstruction(graph, ReturnStmt.class, "return second();");
        assertEquals(EdgeKind.FINALLY, first.outgoingEdges().getFirst().kind());
        assertEquals(List.of(EdgeKind.RETURN), second.outgoingEdges().stream()
                .map(CfgEdge::kind)
                .toList());
        assertEquals(graph.normalExit(), second.outgoingEdges().getFirst().target());
    }

    @Test
    void routesBreakThroughFinallyButNotThroughOuterFinally() {
        ControlFlowGraph graph = graph("""
                void example(boolean condition) {
                    try {
                        while (condition) {
                            try {
                                break;
                            } finally {
                                innerCleanup();
                            }
                        }
                        afterLoop();
                    } finally {
                        outerCleanup();
                    }
                }
                """);

        BasicBlock breakBlock = blockWithInstruction(graph, BreakStmt.class, "break;");
        assertEquals(EdgeKind.FINALLY, breakBlock.outgoingEdges().getFirst().kind());

        BasicBlock innerCleanup = graph.blocks().stream()
                .filter(block -> block.instructions().stream().anyMatch(instruction ->
                        instruction.source().toString().equals("innerCleanup();")))
                .filter(block -> block.outgoingEdges().stream().anyMatch(edge -> edge.kind() == EdgeKind.BREAK))
                .findFirst()
                .orElseThrow();
        assertFalse(innerCleanup.outgoingEdges().stream().anyMatch(edge -> edge.kind() == EdgeKind.FINALLY));
    }

    @Test
    void explicitThrowHasCaughtAndUncaughtPaths() {
        ControlFlowGraph graph = graph("""
                void example() {
                    try {
                        throw new IllegalStateException();
                    } catch (RuntimeException exception) {
                        recover();
                    }
                }
                """);

        BasicBlock throwing = blockWithInstruction(
                graph,
                ThrowStmt.class,
                "throw new IllegalStateException();"
        );
        assertTrue(throwing.outgoingEdges().stream().anyMatch(edge -> edge.kind() == EdgeKind.EXCEPTION));
        assertTrue(throwing.outgoingEdges().stream().anyMatch(edge ->
                edge.kind() == EdgeKind.THROW && edge.target().equals(graph.exceptionalExit())));
    }

    @Test
    void closesResourcesInReverseOrderBeforeCatch() {
        ControlFlowGraph graph = graph("""
                void example() {
                    try (var first = open(); var second = open()) {
                        throw new RuntimeException();
                    } catch (RuntimeException exception) {
                        recover();
                    }
                }
                """);

        BasicBlock throwing = blockWithInstruction(
                graph,
                ThrowStmt.class,
                "throw new RuntimeException();"
        );
        BasicBlock secondClose = graph.block(edge(throwing, EdgeKind.FINALLY).target());
        assertEquals(CfgInstruction.Kind.RESOURCE_CLOSE, secondClose.instructions().getFirst().kind());
        assertTrue(secondClose.instructions().getFirst().source().toString().contains("second"));

        BasicBlock firstClose = graph.block(edge(secondClose, EdgeKind.FINALLY).target());
        assertEquals(CfgInstruction.Kind.RESOURCE_CLOSE, firstClose.instructions().getFirst().kind());
        assertTrue(firstClose.instructions().getFirst().source().toString().contains("first"));
        assertTrue(firstClose.outgoingEdges().stream().anyMatch(edge -> edge.kind() == EdgeKind.EXCEPTION));
    }

    @Test
    void exitsMonitorOnReturn() {
        ControlFlowGraph graph = graph("""
                void example(Object lock) {
                    synchronized (lock) {
                        return;
                    }
                }
                """);

        BasicBlock returning = blockWithInstruction(graph, ReturnStmt.class, "return;");
        BasicBlock monitorExit = graph.block(edge(returning, EdgeKind.FINALLY).target());
        assertEquals(CfgInstruction.Kind.MONITOR_EXIT, monitorExit.instructions().getFirst().kind());
        assertEquals(graph.normalExit(), edge(monitorExit, EdgeKind.RETURN).target());
    }

    @Test
    void lowersSwitchExpressionAndYield() {
        ControlFlowGraph graph = graph("""
                int example(int value) {
                    return switch (value) {
                        case 1 -> calculate();
                        default -> {
                            prepare();
                            yield fallback();
                        }
                    };
                }
                """);

        assertTrue(graph.blocks().stream()
                .flatMap(block -> block.outgoingEdges().stream())
                .anyMatch(edge -> edge.kind() == EdgeKind.SWITCH_CASE));
        assertTrue(graph.blocks().stream()
                .flatMap(block -> block.outgoingEdges().stream())
                .anyMatch(edge -> edge.kind() == EdgeKind.YIELD));
        assertTrue(graph.reachableBlocks().contains(graph.normalExit()));
    }

    @Test
    void exportsGraphvizForDebugging() {
        String dot = new CfgDotExporter().export(graph("void example() { work(); }"));
        assertTrue(dot.startsWith("digraph cfg"));
        assertTrue(dot.contains("work();"));
        assertTrue(dot.contains("NORMAL EXIT"));
    }

    private static ControlFlowGraph graph(String methodSource) {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(
                ParserConfiguration.LanguageLevel.JAVA_25
        );
        MethodDeclaration method = StaticJavaParser.parseBodyDeclaration(methodSource)
                .asMethodDeclaration();
        ExecutableScope scope = ExecutableScope.from(method).orElseThrow();
        return new CfgBuilder().build(scope);
    }

    private static BasicBlock blockWithName(ControlFlowGraph graph, String name) {
        return graph.blocks().stream()
                .filter(block -> block.instructions().stream().anyMatch(instruction ->
                        instruction.source() instanceof NameExpr expression
                                && expression.getNameAsString().equals(name)))
                .findFirst()
                .orElseThrow();
    }

    private static BasicBlock blockWithInstruction(
            ControlFlowGraph graph,
            Class<?> type,
            String source
    ) {
        return graph.blocks().stream()
                .filter(block -> block.instructions().stream().anyMatch(instruction ->
                        type.isInstance(instruction.source())
                                && instruction.source().toString().equals(source)))
                .findFirst()
                .orElseThrow();
    }

    private static CfgEdge edge(BasicBlock block, EdgeKind kind) {
        return block.outgoingEdges().stream()
                .filter(edge -> edge.kind() == kind)
                .findFirst()
                .orElseThrow();
    }
}
