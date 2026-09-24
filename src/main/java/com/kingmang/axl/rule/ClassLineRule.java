package com.kingmang.axl.rule;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.problem.Severity;

public final class ClassLineRule extends AstRule {
    public static final String ID = "class-lines";
    public static final int DEFAULT_CLASS_INFO_THRESHOLD = 300;
    public static final int DEFAULT_CLASS_WARNING_THRESHOLD = 500;

    private final int infoClassThreshold;
    private final int warningClassThreshold;

    public ClassLineRule() {
        this(DEFAULT_CLASS_INFO_THRESHOLD, DEFAULT_CLASS_WARNING_THRESHOLD);
    }

    public ClassLineRule(int infoThreshold, int warningThreshold) {
        if (infoThreshold < 0 || warningThreshold <= infoThreshold) {
            throw new IllegalArgumentException("Expected 0 <= infoThreshold < warningThreshold");
        }
        this.infoClassThreshold = infoThreshold;
        this.warningClassThreshold = warningThreshold;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "Reports classes and interfaces that exceed the configured line limits";
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, AnalysisContext context) {
        int count = declaration.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        if (count >= warningClassThreshold) {
            report(declaration, context, Severity.WARNING, warningClassThreshold);
        } else if (count > infoClassThreshold) {
            report(declaration, context, Severity.INFO, infoClassThreshold);
        }

        super.visit(declaration, context);
    }

    private void report(
            ClassOrInterfaceDeclaration declaration,
            AnalysisContext context,
            Severity severity,
            int threshold
    ) {
        collector().report(
                getId(),
                severity,
                "class " + declaration.getNameAsString() + " has more than " + threshold + " lines",
                context.getSourceFile().getPath().toString(),
                getClass().getSimpleName(),
                declaration.getRange()
        );
    }
}
