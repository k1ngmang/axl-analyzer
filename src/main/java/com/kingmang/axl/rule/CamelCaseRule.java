package com.kingmang.axl.rule;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Severity;

public class CamelCaseRule extends AstRule{

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, AnalysisContext context){
        if(
                declaration.getNameAsString().length() <= 2 ||
                Character.isLowerCase(declaration.getNameAsString().charAt(0))
        ){
            collector().report(
                    this,
                    Severity.WARNING,
                    "Class " + declaration.getNameAsString() + " must be in CamelCase with length > 2",
                    context.getSourceFile().getPath().toString(),
                    declaration.getRange()
            );
        }
    }

    @Override
    public String getId() {
        return Constant.CAMEL_CASE_ID;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }
}
