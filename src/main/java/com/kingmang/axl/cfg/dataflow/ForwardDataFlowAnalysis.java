package com.kingmang.axl.cfg.dataflow;

import com.kingmang.axl.cfg.CfgEdge;
import com.kingmang.axl.cfg.CfgInstruction;
import com.kingmang.axl.cfg.ExecutableScope;

import java.util.Collection;

public interface ForwardDataFlowAnalysis<S> {
    S entryState(ExecutableScope scope);

    S join(Collection<S> states);

    S transferInstruction(CfgInstruction instruction, S input);

    default S transferEdge(CfgEdge edge, S input) {
        return input;
    }
}
