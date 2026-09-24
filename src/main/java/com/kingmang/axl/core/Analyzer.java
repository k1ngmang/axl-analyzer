package com.kingmang.axl.core;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.kingmang.axl.problem.ProblemCollector;
import com.kingmang.axl.problem.Severity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class Analyzer {

    private final AnalysisRunContext runContext;

    public Analyzer(AnalysisRunContext runContext) {
        this.runContext = Objects.requireNonNull(runContext, "runContext");
    }

    public Optional<AnalysisContext> analyze(Path source) throws IOException {
        Path normalizedSource = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        SourceFile sourceFile = new SourceFile(normalizedSource, Files.readString(normalizedSource));
        ParseResult<CompilationUnit> result = runContext.getParser().parse(sourceFile.getText());
        ProblemCollector collector = runContext.getCollector();

        result.getProblems().forEach(parseProblem -> collector.report(
                Constant.PARSER_RULE_ID,
                Severity.ERROR,
                parseProblem.getMessage(),
                sourceFile.getPath().toString(),
                "JavaParser",
                parseProblem.getLocation()
                        .flatMap(location -> location.getBegin().getRange())
        ));

        return result.getResult().map(compilationUnit -> {
            AnalysisContext context = new AnalysisContext(sourceFile, compilationUnit);
            runContext.getRules().stream()
                    .filter(rule -> runContext.getConfig().isRuleEnabled(rule.getId()))
                    .forEach(rule -> rule.analyze(context, collector));
            return context;
        });
    }
}
