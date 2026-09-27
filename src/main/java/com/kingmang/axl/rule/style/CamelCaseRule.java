package com.kingmang.axl.rule.style;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.rule.AstRule;

public class CamelCaseRule extends AstRule {

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, AnalysisContext context){
        if(
                declaration.getNameAsString().length() <= 2 ||
                !isPascal(declaration.getNameAsString())
        ){
            collector().report(
                    this,
                    Severity.WARNING,
                    "Class " + declaration.getNameAsString() + " must be in PascalCase with length > 2",
                    context.getSourceFile().getPath().toString(),
                    declaration.getRange()
            );
        }
        super.visit(declaration, context);
    }

    @Override
    public void visit(MethodDeclaration declaration, AnalysisContext context) {
        if(
                declaration.getNameAsString().length() <= 2 ||
                !isCamel(declaration.getNameAsString())
        ){
            collector().report(
                    this,
                    Severity.WARNING,
                    "Method " + declaration.getNameAsString() + " must be in camelCase with length > 2",
                    context.getSourceFile().getPath().toString(),
                    declaration.getRange()
            );
        }
        super.visit(declaration, context);
    }

    private static boolean isCamel(String s) {
        if (s.isEmpty() || !Character.isLowerCase(s.charAt(0))) return false;
        return s.chars().allMatch(Character::isLetterOrDigit);
    }

    private static boolean isPascal(String s) {
        if (s.isEmpty() || !Character.isUpperCase(s.charAt(0))) return false;
        return s.chars().allMatch(Character::isLetterOrDigit);
    }


    @Override
    public String getId() {
        return Constant.CAMEL_CASE_ID;
    }

    @Override
    public String getDescription() {
        return "Checks for compliance with naming conventions";
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }
}
