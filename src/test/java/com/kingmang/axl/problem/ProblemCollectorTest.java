package com.kingmang.axl.problem;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProblemCollectorTest {
    @Test
    void snapshotDoesNotChangeWhenNewProblemsAreReported() {
        ProblemCollector collector = new ProblemCollector();
        collector.report("first", Severity.INFO, "message", "Example.java", Optional.empty());

        var snapshot = collector.snapshot();
        collector.report("second", Severity.ERROR, "message", "Example.java", Optional.empty());

        assertEquals(1, snapshot.size());
        assertEquals(2, collector.getProblems().size());
    }
}
