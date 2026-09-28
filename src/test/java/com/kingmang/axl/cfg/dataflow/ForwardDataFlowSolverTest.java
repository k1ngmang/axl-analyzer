package com.kingmang.axl.cfg.dataflow;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.kingmang.axl.cfg.CfgBuilder;
import com.kingmang.axl.cfg.CfgEdge;
import com.kingmang.axl.cfg.CfgInstruction;
import com.kingmang.axl.cfg.ControlFlowGraph;
import com.kingmang.axl.cfg.ExecutableScope;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForwardDataFlowSolverTest {
    @Test
    void reachesFixpointAcrossLoopAndJoinsBranches() {
        MethodDeclaration method = StaticJavaParser.parseBodyDeclaration("""
                void example(boolean repeat) {
                    start();
                    while (repeat) {
                        body();
                    }
                    finish();
                }
                """).asMethodDeclaration();
        ControlFlowGraph graph = new CfgBuilder().build(ExecutableScope.from(method).orElseThrow());

        DataFlowResult<Set<String>> result = new ForwardDataFlowSolver().solve(
                graph,
                new ReachingSources()
        );

        Set<String> exit = result.input(graph.normalExit());
        assertTrue(exit.contains("start();"));
        assertTrue(exit.contains("finish();"));
        assertTrue(exit.contains("body();"));
        assertEquals(graph.reachableBlocks().size(), result.inputStates().size());
    }

    private static final class ReachingSources implements ForwardDataFlowAnalysis<Set<String>> {
        @Override
        public Set<String> entryState(ExecutableScope scope) {
            return Set.of();
        }

        @Override
        public Set<String> join(Collection<Set<String>> states) {
            Set<String> result = new LinkedHashSet<>();
            states.forEach(result::addAll);
            return Set.copyOf(result);
        }

        @Override
        public Set<String> transferInstruction(CfgInstruction instruction, Set<String> input) {
            Set<String> result = new LinkedHashSet<>(input);
            result.add(instruction.source().toString());
            return Set.copyOf(result);
        }

        @Override
        public Set<String> transferEdge(CfgEdge edge, Set<String> input) {
            return input;
        }
    }
}
