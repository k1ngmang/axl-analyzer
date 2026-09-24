package com.kingmang.axl.rule;

import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.problem.ProblemCollector;

public abstract class AstRule extends VoidVisitorAdapter<AnalysisContext> implements Rule {
    private ProblemCollector collector;

    @Override
    public final void analyze(AnalysisContext context, ProblemCollector collector) {
        this.collector = collector;
        try {
            context.getCompilationUnit().accept(this, context);
        } finally {
            this.collector = null;
        }
    }

    protected final ProblemCollector collector() {
        if (collector == null) {
            throw new IllegalStateException("Rule is not currently analyzing a source file");
        }
        return collector;
    }
}
