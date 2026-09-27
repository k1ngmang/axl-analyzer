package com.kingmang.axl.rule.style;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.rule.AstRule;

public class BoolMethodNameRule extends AstRule {

    @Override
    public void visit(MethodDeclaration fd, AnalysisContext context){
        if(
                !fd.getType().toString().equals("boolean")
                || fd.getNameAsString().toLowerCase().startsWith("is")
                || fd.getNameAsString().toLowerCase().startsWith("are")
                || fd.getNameAsString().toLowerCase().startsWith("should")
                || fd.getNameAsString().toLowerCase().startsWith("has")
                || fd.getNameAsString().toLowerCase().startsWith("whether")
                || fd.getNameAsString().toLowerCase().startsWith("would")
                || fd.getNameAsString().toLowerCase().startsWith("were")
                || fd.getNameAsString().toLowerCase().startsWith("will")
                || fd.getNameAsString().toLowerCase().startsWith("can")
                || fd.getNameAsString().toLowerCase().startsWith("could")
        )
            return;

        collector().report(
                this,
                Severity.INFO,
                "The method " + fd.getNameAsString() + " does not begin with a question word.",
                context.getSourceFile().getPath().toString(),
                fd.getRange()
        );
        super.visit(fd, context);
    }

    @Override
    public String getId() {
        return Constant.BOOLEAN_NAME_ID;
    }

    @Override
    public String getDescription() {
        return "Reports boolean declarations that do not start with a question";
    }

    @Override
    public Severity getSeverity() {
        return Severity.INFO;
    }
}
