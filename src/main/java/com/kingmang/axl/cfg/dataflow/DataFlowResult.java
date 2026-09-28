package com.kingmang.axl.cfg.dataflow;

import com.kingmang.axl.cfg.BlockId;

import java.util.Map;
import java.util.Objects;

public record DataFlowResult<S>(Map<BlockId, S> inputStates, Map<BlockId, S> outputStates) {
    public DataFlowResult {
        inputStates = Map.copyOf(Objects.requireNonNull(inputStates, "inputStates"));
        outputStates = Map.copyOf(Objects.requireNonNull(outputStates, "outputStates"));
    }

    public S input(BlockId block) {
        return inputStates.get(block);
    }

    public S output(BlockId block) {
        return outputStates.get(block);
    }
}
