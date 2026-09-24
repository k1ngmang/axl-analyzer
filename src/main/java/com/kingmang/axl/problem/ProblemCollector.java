package com.kingmang.axl.problem;

import com.github.javaparser.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ProblemCollector {
    private final List<Problem> problems = new ArrayList<>();

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

    // returns an unmodifiable view which reflects later reports
    public List<Problem> getProblems() {
        return Collections.unmodifiableList(problems);
    }

    // returns an immutable point-in-time copy
    public List<Problem> snapshot() {
        return List.copyOf(problems);
    }
}
