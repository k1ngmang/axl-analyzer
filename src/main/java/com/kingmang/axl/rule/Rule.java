package com.kingmang.axl.rule;

import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.problem.ProblemCollector;
import com.kingmang.axl.problem.Severity;

public interface Rule {
    String getDescription();

    Severity getSeverity();

    void analyze(AnalysisContext context, ProblemCollector collector);
}
