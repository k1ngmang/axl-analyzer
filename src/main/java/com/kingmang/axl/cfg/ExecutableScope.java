package com.kingmang.axl.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.stmt.Statement;

import java.util.Objects;
import java.util.Optional;

public record ExecutableScope(Node declaration, Statement body, Kind kind) {
    public enum Kind {
        METHOD,
        CONSTRUCTOR,
        COMPACT_CONSTRUCTOR,
        LAMBDA,
        INITIALIZER
    }

    public ExecutableScope {
        Objects.requireNonNull(declaration, "declaration");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(kind, "kind");
    }

    public static Optional<ExecutableScope> from(Node node) {
        if (node instanceof MethodDeclaration method) {
            return method.getBody().map(body -> new ExecutableScope(method, body, Kind.METHOD));
        }
        if (node instanceof ConstructorDeclaration constructor) {
            return Optional.of(new ExecutableScope(constructor, constructor.getBody(), Kind.CONSTRUCTOR));
        }
        if (node instanceof CompactConstructorDeclaration constructor) {
            return Optional.of(new ExecutableScope(
                    constructor,
                    constructor.getBody(),
                    Kind.COMPACT_CONSTRUCTOR
            ));
        }
        if (node instanceof LambdaExpr lambda) {
            return Optional.of(new ExecutableScope(lambda, lambda.getBody(), Kind.LAMBDA));
        }
        if (node instanceof InitializerDeclaration initializer) {
            return Optional.of(new ExecutableScope(
                    initializer,
                    initializer.getBody(),
                    Kind.INITIALIZER
            ));
        }
        return Optional.empty();
    }
}
