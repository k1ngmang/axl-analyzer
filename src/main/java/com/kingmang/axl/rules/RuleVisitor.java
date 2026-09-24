package com.kingmang.axl.rules;

import com.github.javaparser.ast.CompilationUnit;

public interface RuleVisitor {
    void visit(CompilationUnit compilationUnit);
}
