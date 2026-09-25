package com.kingmang.axl.core;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.kingmang.axl.problem.ProblemCollector;
import com.kingmang.axl.rule.Rule;

import java.util.List;
import java.util.Objects;

public final class AnalysisRunContext {
    private final JavaParser parser;
    private final List<Rule> rules;
    private final ProblemCollector collector;
    private final Config config;

    public AnalysisRunContext(List<? extends Rule> rules) {
        this(rules, new Config());
    }

    public AnalysisRunContext(List<? extends Rule> rules, Config config) {
        this(
                new JavaParser(new ParserConfiguration().setLanguageLevel(
                        ParserConfiguration.LanguageLevel.JAVA_25
                )),
                rules,
                new ProblemCollector(),
                config
        );
    }

    public AnalysisRunContext(JavaParser parser, List<? extends Rule> rules, ProblemCollector collector) {
        this(parser, rules, collector, new Config());
    }

    public AnalysisRunContext(
            JavaParser parser,
            List<? extends Rule> rules,
            ProblemCollector collector,
            Config config
    ) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        this.collector = Objects.requireNonNull(collector, "collector");
        this.config = Objects.requireNonNull(config, "config");
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

    public Config getConfig() {
        return config;
    }
}
