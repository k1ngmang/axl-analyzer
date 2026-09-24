package com.kingmang.axl.core;

import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.rule.ClassLineRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void runsEnabledRulesAndPreservesSourceLocation() throws IOException {
        Path source = temporaryDirectory.resolve("Large.java");
        Files.writeString(source, "class Large {\n\n\n}\n");
        AnalysisRunContext runContext = new AnalysisRunContext(List.of(new ClassLineRule(1, 3, 4, 5)));

        new Analyzer(runContext).analyze(source);

        var problem = runContext.getCollector().getProblems().getFirst();
        assertEquals(Constant.CLASS_LINE_ID, problem.getRuleId());
        assertEquals(Severity.WARNING, problem.getSeverity());
        assertEquals(source.toAbsolutePath().normalize().toString(), problem.getFileName());
        assertTrue(problem.getRange().isPresent());
    }

    @Test
    void skipsDisabledRules() throws IOException {
        Path source = temporaryDirectory.resolve("Large.java");
        Files.writeString(source, "class Large {\n\n\n}\n");
        ClassLineRule rule = new ClassLineRule(1, 3, 4, 5);
        Config config = new Config(Map.of(Constant.CLASS_LINE_ID, false));
        AnalysisRunContext runContext = new AnalysisRunContext(List.of(rule), config);

        new Analyzer(runContext).analyze(source);

        assertTrue(runContext.getCollector().getProblems().isEmpty());
    }

    @Test
    void reportsLongMethodsWithTheirNameAndRange() throws IOException {
        Path source = temporaryDirectory.resolve("Example.java");
        Files.writeString(source, """
                class Example {
                    void longMethod() {


                    }
                }
                """);
        AnalysisRunContext runContext = new AnalysisRunContext(
                List.of(new ClassLineRule(100, 200, 1, 3))
        );

        new Analyzer(runContext).analyze(source);

        var problems = runContext.getCollector().getProblems();
        assertEquals(1, problems.size());
        assertEquals(Severity.WARNING, problems.getFirst().getSeverity());
        assertTrue(problems.getFirst().getMessage().startsWith("method longMethod "));
        assertTrue(problems.getFirst().getRange().isPresent());
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
