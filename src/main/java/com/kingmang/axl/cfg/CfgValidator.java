package com.kingmang.axl.cfg;

import java.util.HashSet;
import java.util.Set;

public final class CfgValidator {
    public void validate(ControlFlowGraph graph) {
        Set<BlockId> ids = new HashSet<>();
        for (BasicBlock block : graph.blocks()) {
            if (!ids.add(block.id())) {
                throw new IllegalStateException("Duplicate block " + block.id());
            }
            for (CfgEdge edge : block.outgoingEdges()) {
                if (!edge.source().equals(block.id())) {
                    throw new IllegalStateException("Edge has the wrong source block: " + edge);
                }
                graph.block(edge.target());
            }
        }
        if (!graph.block(graph.normalExit()).isTerminal()) {
            throw new IllegalStateException("Normal exit must be terminal");
        }
        if (!graph.block(graph.exceptionalExit()).isTerminal()) {
            throw new IllegalStateException("Exceptional exit must be terminal");
        }
    }
}
