package com.kingmang.axl.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;

import java.util.Objects;
import java.util.Optional;

public final class AnalysisContext {
    private final SourceFile sourceFile;
    private final CompilationUnit compilationUnit;
    private final JavaParserFacade javaParserFacade;

    public AnalysisContext(SourceFile sourceFile, CompilationUnit compilationUnit) {
        this(sourceFile, compilationUnit, null);
    }

    public AnalysisContext(
            SourceFile sourceFile,
            CompilationUnit compilationUnit,
            JavaParserFacade javaParserFacade
    ) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.compilationUnit = Objects.requireNonNull(compilationUnit, "compilationUnit");
        this.javaParserFacade = javaParserFacade;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public Optional<JavaParserFacade> getJavaParserFacade() {
        return Optional.ofNullable(javaParserFacade);
    }
}
