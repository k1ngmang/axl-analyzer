package com.kingmang.axl.rule;

import com.kingmang.axl.core.AnalysisRunContext;
import com.kingmang.axl.core.Analyzer;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Problem;
import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.rule.sema.UnreachableCodeRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnreachableCodeRuleTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void groupsAdjacentStatementsAfterReturn() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    void run() {
                        return;
                        first();
                        second();
                    }
                }
                """);

        assertEquals(1, problems.size());
        Problem problem = problems.getFirst();
        assertEquals(Constant.UNREACHABLE_CODE_ID, problem.getRuleId());
        assertEquals(Severity.WARNING, problem.getSeverity());
        assertEquals("Unreachable code.", problem.getMessage());
        assertEquals(4, problem.getRange().orElseThrow().begin.line);
        assertEquals(5, problem.getRange().orElseThrow().end.line);
    }

    @Test
    void reportsNestedFragmentAfterAbruptCompletion() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    void run(boolean condition) {
                        if (condition) {
                            return;
                            nested();
                        }
                        reachable();
                    }
                }
                """);

        assertEquals(1, problems.size());
        assertEquals(5, problems.getFirst().getRange().orElseThrow().begin.line);
    }

    @Test
    void analyzesLambdaAsIndependentExecutableScope() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    void run() {
                        Runnable action = () -> {
                            return;
                            nested();
                        };
                        reachable();
                    }
                }
                """);

        assertEquals(1, problems.size());
        assertEquals(5, problems.getFirst().getRange().orElseThrow().begin.line);
    }

    @Test
    void doesNotTreatCatchBodyAsUnreachableWithoutImplicitExceptionEdges() throws IOException {
        assertTrue(analyze("""
                class Example {
                    void run() {
                        try {
                            work();
                        } catch (RuntimeException exception) {
                            recover();
                        }
                    }
                }
                """).isEmpty());
    }

    @Test
    void reportsAbruptlyUnreachableCodeInsideCatch() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    void run() {
                        try {
                            work();
                        } catch (RuntimeException exception) {
                            return;
                            unreachable();
                        }
                    }
                }
                """);

        assertEquals(1, problems.size());
        assertEquals(7, problems.getFirst().getRange().orElseThrow().begin.line);
    }

    @Test
    void reportsWholeTryStatementWhenItFollowsReturn() throws IOException {
        List<Problem> problems = analyze("""
                class Example {
                    void run() {
                        return;
                        try {
                            work();
                        } catch (RuntimeException exception) {
                            recover();
                        }
                    }
                }
                """);

        assertEquals(1, problems.size());
        assertEquals(4, problems.getFirst().getRange().orElseThrow().begin.line);
        assertEquals(8, problems.getFirst().getRange().orElseThrow().end.line);
    }

    private List<Problem> analyze(String sourceText) throws IOException {
        Path source = temporaryDirectory.resolve("Example.java");
        Files.writeString(source, sourceText);
        AnalysisRunContext context = new AnalysisRunContext(List.of(new UnreachableCodeRule()));

        new Analyzer(context).analyze(source);

        return context.getCollector().getProblems();
    }
}
