package com.kingmang.axl.rule;

import com.kingmang.axl.core.AnalysisRunContext;
import com.kingmang.axl.core.Analyzer;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Problem;
import com.kingmang.axl.problem.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CyclomaticComplexityRuleTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsMethodsAndConstructorsIndependently() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    Example(boolean a) { if (a) {} }
                    void simple() {}
                    void complex(boolean a, boolean b) { if (a && b) {} }
                }
                """, 1, 3);

        assertEquals(2, problems.size());
        assertEquals(Severity.INFO, problems.get(0).getSeverity());
        assertTrue(problems.get(0).getMessage().startsWith("constructor Example "));
        assertEquals(Severity.WARNING, problems.get(1).getSeverity());
        assertTrue(problems.get(1).getMessage().startsWith("method complex "));
        assertTrue(problems.stream().allMatch(problem -> problem.getRange().isPresent()));
    }

    @Test
    void reportsNestedClassMethodSeparately() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    void outer(boolean condition) {
                        class Local {
                            void nested() { if (condition) {} }
                        }
                    }
                }
                """, 1, 3);

        assertEquals(1, problems.size());
        assertTrue(problems.getFirst().getMessage().startsWith("method nested "));
        assertTrue(problems.getFirst().getMessage().contains("complexity 2"));
    }

    @Test
    void reportsCompactRecordConstructor() throws IOException {
        List<Problem> problems = analyze("""
                record Example(boolean enabled) {
                    Example { if (enabled) {} }
                }
                """, 1, 3);

        assertEquals(1, problems.size());
        assertTrue(problems.getFirst().getMessage().startsWith("constructor Example "));
        assertTrue(problems.getFirst().getMessage().contains("complexity 2"));
    }

    private List<Problem> analyze(String sourceText, int infoThreshold, int warningThreshold)
            throws IOException {
        Path source = temporaryDirectory.resolve("Example.java");
        Files.writeString(source, sourceText);
        AnalysisRunContext context = new AnalysisRunContext(
                List.of(new CyclomaticComplexityRule(infoThreshold, warningThreshold))
        );

        new Analyzer(context).analyze(source);

        assertTrue(context.getCollector().getProblems().stream()
                .allMatch(problem -> problem.getRuleId().equals(Constant.CYCLOMATIC_COMPLEX_ID)),
                () -> "Unexpected problems: " + context.getCollector().getProblems());
        return context.getCollector().getProblems();
    }
}
