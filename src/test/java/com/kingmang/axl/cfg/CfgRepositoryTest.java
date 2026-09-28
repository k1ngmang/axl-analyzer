package com.kingmang.axl.cfg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CfgRepositoryTest {
    @Test
    void discoversNestedScopesButBuildsSeparateGraphs() {
        CompilationUnit unit = StaticJavaParser.parse("""
                class Example {
                    { initialize(); }
                    void run() {
                        Runnable action = () -> execute();
                    }
                }
                """);
        CfgRepository repository = new CfgRepository(unit);

        assertEquals(3, repository.scopes().size());
        for (ExecutableScope scope : repository.scopes()) {
            assertSame(repository.get(scope), repository.get(scope));
            assertSame(scope.declaration(), repository.get(scope).scope().declaration());
        }
        assertEquals(3, repository.materializedGraphs().size());
    }
}
