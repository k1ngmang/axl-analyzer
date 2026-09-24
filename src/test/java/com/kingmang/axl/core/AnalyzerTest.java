package com.kingmang.axl.core;

import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.rule.ClassLineRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void runsEnabledRulesAndPreservesSourceLocation() throws IOException {
        Path source = temporaryDirectory.resolve("Large.java");
        Files.writeString(source, "class Large {\n\n\n}\n");
        AnalysisRunContext runContext = new AnalysisRunContext(List.of(new ClassLineRule(1, 3)));

        new Analyzer(runContext).analyze(source);

        var problem = runContext.getCollector().getProblems().getFirst();
        assertEquals(ClassLineRule.ID, problem.getRuleId());
        assertEquals(Severity.WARNING, problem.getSeverity());
        assertEquals(source.toAbsolutePath().normalize().toString(), problem.getFileName());
        assertTrue(problem.getRange().isPresent());
    }

    @Test
    void skipsDisabledRules() throws IOException {
        Path source = temporaryDirectory.resolve("Large.java");
        Files.writeString(source, "class Large {\n\n\n}\n");
        ClassLineRule rule = new ClassLineRule(1, 3);
        rule.setEnabled(false);
        AnalysisRunContext runContext = new AnalysisRunContext(List.of(rule));

        new Analyzer(runContext).analyze(source);

        assertTrue(runContext.getCollector().getProblems().isEmpty());
    }

    @Test
    void collectorsAreIsolatedBetweenRuns() throws IOException {
        Path invalidSource = temporaryDirectory.resolve("Broken.java");
        Files.writeString(invalidSource, "class {");
        AnalysisRunContext firstRun = new AnalysisRunContext(List.of());
        AnalysisRunContext secondRun = new AnalysisRunContext(List.of());

        new Analyzer(firstRun).analyze(invalidSource);

        assertTrue(firstRun.getCollector().getProblems().stream()
                .anyMatch(problem -> problem.getSeverity() == Severity.ERROR));
        assertTrue(secondRun.getCollector().getProblems().isEmpty());
    }
}
