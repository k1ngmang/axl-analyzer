package com.kingmang.axl.problem;

import com.github.javaparser.Range;
import com.kingmang.axl.core.RuleIdResolver;
import com.kingmang.axl.rule.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProblemCollector {
    private final List<Problem> problems = new ArrayList<>();
    private final RuleIdResolver ruleIds;

    public ProblemCollector() {
        this(new RuleIdResolver());
    }

    public ProblemCollector(RuleIdResolver ruleIds) {
        this.ruleIds = Objects.requireNonNull(ruleIds, "ruleIds");
    }

    public void report(Problem problem) {
        problems.add(Objects.requireNonNull(problem, "problem"));
    }

    public void report(
            String ruleId,
            Severity severity,
            String message,
            String source,
            Range range
    ) {
        report(ruleId, severity, message, source, Optional.ofNullable(range));
    }

    public void report(
            String ruleId,
            Severity severity,
            String message,
            String source,
            Optional<Range> range
    ) {
        report(ruleId, severity, message, source, ruleId, range);
    }

    public void report(
            String ruleId,
            Severity severity,
            String message,
            String source,
            String ruleKind,
            Optional<Range> range
    ) {
        report(new Problem(ruleId, severity, message, source, ruleKind, range));
    }

    public void report(
            Rule rule,
            Severity severity,
            String message,
            String source,
            Optional<Range> range
    ) {
        Objects.requireNonNull(rule, "rule");
        report(
                ruleIds.getId(rule.getClass()),
                severity,
                message,
                source,
                rule.getClass().getSimpleName(),
                range
        );
    }

    // returns an unmodifiable view which reflects later reports
    public List<Problem> getProblems() {
        return Collections.unmodifiableList(problems);
    }

    // returns an immutable point-in-time copy
    public List<Problem> snapshot() {
        return List.copyOf(problems);
    }
}
