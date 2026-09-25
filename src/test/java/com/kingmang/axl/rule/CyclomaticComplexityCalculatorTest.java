package com.kingmang.axl.rule;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CyclomaticComplexityCalculatorTest {
    private final CyclomaticComplexityCalculator calculator = new CyclomaticComplexityCalculator();

    @Test
    void emptyMethodHasBaseComplexity() {
        assertEquals(1, calculate("void example() {}"));
    }

    @Test
    void countsJavaDecisionPoints() {
        assertEquals(10, calculate("""
                void example(boolean a, boolean b, int value) {
                    if (a && b || value > 0) {}
                    for (int i = 0; i < value; i++) {}
                    while (a) {}
                    do {} while (b);
                    int result = a ? 1 : 0;
                    try {} catch (RuntimeException exception) {}
                    switch (value) {
                        case 1 -> result++;
                        default -> result--;
                    }
                }
                """));
    }

    @Test
    void countsElseIfAsAnotherDecision() {
        assertEquals(3, calculate("""
                void example(int value) {
                    if (value == 1) {
                    } else if (value == 2) {
                    } else {
                    }
                }
                """));
    }

    @Test
    void excludesNestedExecutableScopes() {
        assertEquals(2, calculate("""
                void example(boolean condition) {
                    if (condition) {}
                    Runnable lambda = () -> { if (condition) {} };
                    class Local { void nested() { if (condition) {} } }
                    Object object = new Object() {
                        void nested() { if (condition) {} }
                    };
                }
                """));
    }

    private int calculate(String source) {
        MethodDeclaration method = StaticJavaParser.parseBodyDeclaration(source)
                .asMethodDeclaration();
        return calculator.calculate(method.getBody().orElseThrow());
    }
}
