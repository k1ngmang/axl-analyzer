package com.kingmang.axl.rule;

import com.kingmang.axl.cfg.ControlFlowGraph;
import com.kingmang.axl.cfg.ExecutableScope;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.problem.ProblemCollector;

public abstract class CfgRule implements Rule {
    @Override
    public final void analyze(AnalysisContext context, ProblemCollector collector) {
        for (ExecutableScope scope : context.getCfgRepository().scopes()) {
            analyze(scope, context.getCfgRepository().get(scope), context, collector);
        }
    }

    protected abstract void analyze(
            ExecutableScope scope,
            ControlFlowGraph graph,
            AnalysisContext context,
            ProblemCollector collector
    );
}
