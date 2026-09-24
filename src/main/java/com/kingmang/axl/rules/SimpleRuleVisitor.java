package com.kingmang.axl.rules;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class SimpleRuleVisitor extends VoidVisitorAdapter<Void> implements RuleVisitor {
    @Override
    public void visit(CompilationUnit compilationUnit) {
        compilationUnit.accept(this, null);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
        System.out.println("class: " + declaration.getNameAsString());
        super.visit(declaration, arg);
    }

    @Override
    public void visit(MethodDeclaration declaration, Void arg) {
        System.out.println("method: " + declaration.getNameAsString());
        super.visit(declaration, arg);
    }
}
