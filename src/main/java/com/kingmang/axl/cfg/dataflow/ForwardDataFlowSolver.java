package com.kingmang.axl.cfg.dataflow;

import com.kingmang.axl.cfg.BasicBlock;
import com.kingmang.axl.cfg.BlockId;
import com.kingmang.axl.cfg.CfgEdge;
import com.kingmang.axl.cfg.CfgInstruction;
import com.kingmang.axl.cfg.ControlFlowGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ForwardDataFlowSolver {
    public <S> DataFlowResult<S> solve(
            ControlFlowGraph graph,
            ForwardDataFlowAnalysis<S> analysis
    ) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(analysis, "analysis");

        Map<BlockId, S> inputs = new LinkedHashMap<>();
        Map<BlockId, S> outputs = new LinkedHashMap<>();
        ArrayDeque<BlockId> work = new ArrayDeque<>();
        Set<BlockId> queued = new HashSet<>();
        work.add(graph.entry());
        queued.add(graph.entry());

        while (!work.isEmpty()) {
            BlockId id = work.removeFirst();
            queued.remove(id);
            BasicBlock block = graph.block(id);

            S input;
            if (id.equals(graph.entry())) {
                input = analysis.entryState(graph.scope());
            } else {
                List<S> predecessorStates = new ArrayList<>();
                for (CfgEdge edge : graph.predecessors(id)) {
                    S predecessorOutput = outputs.get(edge.source());
                    if (predecessorOutput != null) {
                        predecessorStates.add(analysis.transferEdge(edge, predecessorOutput));
                    }
                }
                if (predecessorStates.isEmpty()) {
                    continue;
                }
                input = analysis.join(predecessorStates);
            }

            S output = input;
            for (CfgInstruction instruction : block.instructions()) {
                output = analysis.transferInstruction(instruction, output);
            }

            S previousInput = inputs.put(id, input);
            S previousOutput = outputs.put(id, output);
            if (Objects.equals(previousInput, input) && Objects.equals(previousOutput, output)) {
                continue;
            }
            for (CfgEdge edge : block.outgoingEdges()) {
                if (queued.add(edge.target())) {
                    work.addLast(edge.target());
                }
            }
        }
        return new DataFlowResult<>(inputs, outputs);
    }
}
