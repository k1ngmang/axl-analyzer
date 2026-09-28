package com.kingmang.axl.cfg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Per-compilation-unit lazy cache of executable scopes and their CFGs. */
public final class CfgRepository {
    private final List<ExecutableScope> scopes;
    private final Map<Node, ExecutableScope> scopesByDeclaration = new IdentityHashMap<>();
    private final Map<Node, ControlFlowGraph> graphs = new IdentityHashMap<>();

    public CfgRepository(CompilationUnit compilationUnit) {
        Objects.requireNonNull(compilationUnit, "compilationUnit");
        List<ExecutableScope> discovered = new ArrayList<>();
        compilationUnit.findAll(MethodDeclaration.class).forEach(node -> add(node, discovered));
        compilationUnit.findAll(ConstructorDeclaration.class).forEach(node -> add(node, discovered));
        compilationUnit.findAll(CompactConstructorDeclaration.class).forEach(node -> add(node, discovered));
        compilationUnit.findAll(InitializerDeclaration.class).forEach(node -> add(node, discovered));
        compilationUnit.findAll(LambdaExpr.class).forEach(node -> add(node, discovered));
        scopes = List.copyOf(discovered);
    }

    public List<ExecutableScope> scopes() {
        return scopes;
    }

    public ControlFlowGraph get(Node declaration) {
        Objects.requireNonNull(declaration, "declaration");
        ExecutableScope executableScope = scopesByDeclaration.get(declaration);
        if (executableScope == null) {
            throw new IllegalArgumentException("Node is not an executable scope in this compilation unit");
        }
        return graphs.computeIfAbsent(declaration, ignored -> new CfgBuilder().build(executableScope));
    }

    public ControlFlowGraph get(ExecutableScope executableScope) {
        Objects.requireNonNull(executableScope, "executableScope");
        return get(executableScope.declaration());
    }

    public Map<Node, ControlFlowGraph> materializedGraphs() {
        return Collections.unmodifiableMap(new IdentityHashMap<>(graphs));
    }

    private void add(Node declaration, List<ExecutableScope> discovered) {
        ExecutableScope.from(declaration).ifPresent(executableScope -> {
            scopesByDeclaration.put(declaration, executableScope);
            discovered.add(executableScope);
        });
    }
}
