package com.kingmang.axl.rule;

import com.kingmang.axl.core.AnalysisRunContext;
import com.kingmang.axl.core.Analyzer;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Problem;
import com.kingmang.axl.problem.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoolMethodNameRuleTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsBooleanMethodWithoutQuestionWord() throws IOException {
        List<Problem> problems = analyze("boolean enabled() { return true; }");

        assertEquals(1, problems.size());
        Problem problem = problems.getFirst();
        assertEquals(Constant.BOOLEAN_NAME_ID, problem.getRuleId());
        assertEquals(Severity.INFO, problem.getSeverity());
        assertEquals(
                "The method enabled does not begin with a question word.",
                problem.getMessage()
        );
        assertTrue(problem.getRange().isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "isEnabled", "areEnabled", "shouldEnable", "hasAccess", "whetherEnabled",
            "wouldEnable", "wereEnabled", "willEnable", "canEnable", "couldEnable"
    })
    void acceptsBooleanMethodWithQuestionWord(String methodName) throws IOException {
        assertTrue(analyze("boolean " + methodName + "() { return true; }").isEmpty());
    }

    @Test
    void ignoresNonBooleanMethod() throws IOException {
        assertTrue(analyze("String enabled() { return \"yes\"; }").isEmpty());
    }

    private List<Problem> analyze(String method) throws IOException {
        Path source = temporaryDirectory.resolve("Example.java");
        Files.writeString(source, "class Example { " + method + " }");
        AnalysisRunContext context = new AnalysisRunContext(List.of(new BoolMethodNameRule()));

        new Analyzer(context).analyze(source);

        return context.getCollector().getProblems();
    }
}
