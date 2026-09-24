package com.kingmang.axl.rule;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Severity;

public final class ClassLineRule extends AstRule {
    public static final String ID = "class-lines";

    private final int infoClassThreshold;
    private final int warningClassThreshold;

    private final int infoMethodThreshold;
    private final int warningMethodThreshold;

    public ClassLineRule() {
        this(
                Constant.DEFAULT_CLASS_INFO_THRESHOLD,
                Constant.DEFAULT_CLASS_WARNING_THRESHOLD,
                Constant.DEFAULT_METHOD_INFO_THRESHOLD,
                Constant.DEFAULT_METHOD_WARNING_THRESHOLD
        );
    }

    public ClassLineRule(
            int infoClassThreshold,
            int warningClassThreshold,
            int infoMethodThreshold,
            int warningMethodThreshold
    ) {
        if (infoClassThreshold < 0
                || warningClassThreshold <= infoClassThreshold
                || infoMethodThreshold < 0
                || warningMethodThreshold <= infoMethodThreshold) {
            throw new IllegalArgumentException("Expected 0 <= infoThreshold < warningThreshold");
        }
        this.infoClassThreshold = infoClassThreshold;
        this.warningClassThreshold = warningClassThreshold;
        this.infoMethodThreshold = infoMethodThreshold;
        this.warningMethodThreshold = warningMethodThreshold;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, AnalysisContext context) {
        int count = declaration.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        if (count >= warningClassThreshold) {
            report(
                    declaration,
                    context,
                    Severity.WARNING,
                    warningClassThreshold,
                    declaration.isInterface() ? "interface" : "class"
            );
        } else if (count > infoClassThreshold) {
            report(
                    declaration,
                    context,
                    Severity.INFO,
                    infoClassThreshold,
                    declaration.isInterface() ? "interface" : "class"
            );
        }

        super.visit(declaration, context);
    }

    @Override
    public void visit(MethodDeclaration declaration, AnalysisContext context) {
        int count = declaration.getRange()
                .map(range -> range.end.line - range.begin.line + 1)
                .orElse(0);

        if (count >= warningMethodThreshold) {
            report(declaration, context, Severity.WARNING, warningMethodThreshold, "method");
        } else if (count > infoMethodThreshold) {
            report(declaration, context, Severity.INFO, infoMethodThreshold, "method");
        }

        super.visit(declaration, context);
    }

    private <T extends Node & NodeWithSimpleName<?>> void report(
            T declaration,
            AnalysisContext context,
            Severity severity,
            int threshold,
            String declarationKind
    ) {
        collector().report(
                getId(),
                severity,
                declarationKind + " " + declaration.getNameAsString()
                        + " has more than " + threshold + " lines",
                context.getSourceFile().getPath().toString(),
                getClass().getSimpleName(),
                declaration.getRange()
        );
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDescription() {
        return "Reports classes, interfaces, and methods that exceed the configured line limits";
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }
}
