package com.kingmang.axl;

import com.github.javaparser.JavaParser;
import com.kingmang.axl.display.Display;
import com.kingmang.axl.problem.ProblemList;
import com.kingmang.axl.rules.ClassLineVisitor;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {
    static void main() throws FileNotFoundException {
        JavaParser parser = new JavaParser();
        var result = parser.parse(new File("Test.java"));

        result.getResult().ifPresent(compilationUnit -> {
            //SimpleRuleVisitor visitor = new SimpleRuleVisitor();
            ClassLineVisitor visitor = new ClassLineVisitor();
            visitor.visit(compilationUnit);
        });

        result.getProblems().forEach(problem ->
                System.err.println("parse error: " + problem.getMessage())
        );

        Display.print();
    }
}
