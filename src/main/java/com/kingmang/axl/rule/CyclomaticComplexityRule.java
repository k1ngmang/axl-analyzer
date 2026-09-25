package com.kingmang.axl.rule;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Severity;

public final class CyclomaticComplexityRule extends AstRule {
    private final CyclomaticComplexityCalculator calculator = new CyclomaticComplexityCalculator();
    private final int infoThreshold;
    private final int warningThreshold;

    public CyclomaticComplexityRule() {
        this(
                Constant.DEFAULT_CYCLOMATIC_COMPLEXITY_INFO_THRESHOLD,
                Constant.DEFAULT_CYCLOMATIC_COMPLEXITY_WARNING_THRESHOLD
        );
    }

    public CyclomaticComplexityRule(int infoThreshold, int warningThreshold) {
        if (infoThreshold < 1 || warningThreshold <= infoThreshold) {
            throw new IllegalArgumentException("Expected 1 <= infoThreshold < warningThreshold");
        }
        this.infoThreshold = infoThreshold;
        this.warningThreshold = warningThreshold;
    }

    @Override
    public void visit(MethodDeclaration declaration, AnalysisContext context) {
        declaration.getBody().ifPresent(body -> inspect(
                "method",
                declaration.getNameAsString(),
                body,
                declaration,
                context
        ));
        super.visit(declaration, context);
    }

    @Override
    public void visit(ConstructorDeclaration declaration, AnalysisContext context) {
        inspect(
                "constructor",
                declaration.getNameAsString(),
                declaration.getBody(),
                declaration,
                context
        );
        super.visit(declaration, context);
    }

    @Override
    public void visit(CompactConstructorDeclaration declaration, AnalysisContext context) {
        inspect(
                "constructor",
                declaration.getNameAsString(),
                declaration.getBody(),
                declaration,
                context
        );
        super.visit(declaration, context);
    }

    private void inspect(
            String declarationKind,
            String name,
            Node body,
            Node declaration,
            AnalysisContext context
    ) {
        int complexity = calculator.calculate(body);
        Severity severity;
        int threshold;

        if (complexity >= warningThreshold) {
            severity = Severity.WARNING;
            threshold = warningThreshold;
        } else if (complexity > infoThreshold) {
            severity = Severity.INFO;
            threshold = infoThreshold;
        } else {
            return;
        }

        collector().report(
                this,
                severity,
                declarationKind + " " + name
                        + " has cyclomatic complexity " + complexity
                        + "; configured threshold is " + threshold,
                context.getSourceFile().getPath().toString(),
                declaration.getRange()
        );
    }

    @Override
    public String getId() {
        return Constant.CYCLOMATIC_COMPLEX_ID;
    }

    @Override
    public String getDescription() {
        return "Reports methods and constructors with excessive cyclomatic complexity";
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }
}
