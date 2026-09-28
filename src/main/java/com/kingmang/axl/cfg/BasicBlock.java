package com.kingmang.axl.cfg;

import java.util.List;
import java.util.Objects;

public record BasicBlock(
        BlockId id,
        List<CfgInstruction> instructions,
        List<CfgEdge> outgoingEdges
) {
    public BasicBlock {
        Objects.requireNonNull(id, "id");
        instructions = List.copyOf(Objects.requireNonNull(instructions, "instructions"));
        outgoingEdges = List.copyOf(Objects.requireNonNull(outgoingEdges, "outgoingEdges"));
    }

    public boolean isTerminal() {
        return outgoingEdges.isEmpty();
    }
}
