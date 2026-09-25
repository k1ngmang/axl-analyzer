package com.kingmang.axl.rule;

import com.github.javaparser.ast.stmt.TryStmt;
import com.kingmang.axl.core.AnalysisContext;
import com.kingmang.axl.core.Constant;
import com.kingmang.axl.problem.Severity;

public class EmptyCatchRule extends AstRule{

    @Override
    public void visit(TryStmt ts, AnalysisContext context){
        if(ts.getCatchClauses().isEmpty()){
            collector().report(
                    this,
                    Severity.WARNING,
                    "The expression does not have a catch statement.",
                    context.getSourceFile().getPath().toString(),
                    ts.getRange()
            );
        }
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public String getDescription() {
        return Constant.EMPTY_CATCH_ID;
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }
}
