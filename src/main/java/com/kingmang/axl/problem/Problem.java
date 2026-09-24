package com.kingmang.axl.problem;

import com.github.javaparser.Range;

import java.util.Objects;
import java.util.Optional;

public final class Problem {
    private final String ruleId;
    private final Severity severity;
    private final String message;
    private final String fileName;
    private final String ruleKind;
    private final Optional<Range> range;

    public Problem(
            String ruleId,
            Severity severity,
            String message,
            String fileName,
            String ruleKind,
            Optional<Range> range
    ) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.message = Objects.requireNonNull(message, "message");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.ruleKind = Objects.requireNonNull(ruleKind, "ruleKind");
        this.range = Objects.requireNonNull(range, "range");
    }

    public Problem(String ruleId, Severity severity, String message, String fileName, String ruleKind, Range range) {
        this(ruleId, severity, message, fileName, ruleKind, Optional.ofNullable(range));
    }

    public String getRuleId() {
        return ruleId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRuleKind() {
        return ruleKind;
    }

    public Optional<Range> getRange() {
        return range;
    }

    @Override
    public String toString() {
        String location = range
                .map(value -> ":" + value.begin.line + ":" + value.begin.column)
                .orElse("");
        return fileName + location + " [" + severity + "] " + ruleId + ": " + message;
    }
}
