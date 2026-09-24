package com.kingmang.axl.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.kingmang.axl.display.Display;

public class ClassLineVisitor extends VoidVisitorAdapter<Void> implements RuleVisitor{

    int INFO_MAX_CLASS_LINES = 300; //300 - 500
    int WARN_MAX_CLASS_LINES = 500; //500+
    @Override
    public void visit(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
        //int count = declaration.toString().split("\n").length;

        /*
        I prefer the first option, but the parser collapses line breaks.
        Until I come up with something else, I'll use this solution:
         */
        int count = declaration.getEnd().map(e -> e.line).orElse(0)
                - declaration.getBegin().map(b -> b.line).orElse(0) + 1;
        if(count > INFO_MAX_CLASS_LINES && count < WARN_MAX_CLASS_LINES) {
            //TODO: add this to a list instead of outputting it immediately
            Display.info("class " + declaration.getNameAsString() + " has more than " + INFO_MAX_CLASS_LINES + " lines");
        } else if(count >= WARN_MAX_CLASS_LINES) {
            Display.warn("class " + declaration.getNameAsString() + " has more than " + WARN_MAX_CLASS_LINES + " lines");
        }
        super.visit(declaration, arg);
    }
}
