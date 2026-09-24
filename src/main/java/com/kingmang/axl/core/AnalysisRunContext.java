package com.kingmang.axl.core;

import com.github.javaparser.JavaParser;
import com.kingmang.axl.problem.ProblemCollector;
import com.kingmang.axl.rule.Rule;

import java.util.List;
import java.util.Objects;

public final class AnalysisRunContext {
    private final JavaParser parser;
    private final List<Rule> rules;
    private final ProblemCollector collector;

    public AnalysisRunContext(List<? extends Rule> rules) {
        this(new JavaParser(), rules, new ProblemCollector());
    }

    public AnalysisRunContext(JavaParser parser, List<? extends Rule> rules, ProblemCollector collector) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        this.collector = Objects.requireNonNull(collector, "collector");
    }

    public JavaParser getParser() {
        return parser;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public ProblemCollector getCollector() {
        return collector;
    }
}
