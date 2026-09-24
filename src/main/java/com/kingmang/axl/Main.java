package com.kingmang.axl;

import com.kingmang.axl.core.AnalysisRunContext;
import com.kingmang.axl.core.Analyzer;
import com.kingmang.axl.core.Config;
import com.kingmang.axl.problem.Problem;
import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.report.ConsoleReporter;
import com.kingmang.axl.rule.BoolMethodNameRule;
import com.kingmang.axl.rule.ClassLineRule;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Main {
    static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream output, PrintStream error) {
        if (args.length == 0) {
            error.println("No input paths specified.");
            printUsage(error);
            return 2;
        }

        if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            printUsage(output);
            return 0;
        }

        Config config = new Config(Map.of(
                // Constant.CLASS_LINE_RULE_ID, false
        ));

        AnalysisRunContext runContext = new AnalysisRunContext(
                List.of(
                        new ClassLineRule(),
                        new BoolMethodNameRule()
                ),
                config
        );

        Analyzer analyzer = new Analyzer(runContext);
        try {
            analyze(args, analyzer);
        } catch (IOException | InvalidPathException exception) {
            error.println("Unable to analyze sources: " + exception.getMessage());
            return 2;
        }

        List<Problem> problems = runContext.getCollector().snapshot();
        new ConsoleReporter().report(problems, output);

        return problems.stream()
                .anyMatch(problem -> problem.getSeverity() == Severity.WARNING
                        || problem.getSeverity() == Severity.ERROR)
                ? 1
                : 0;
    }

    private static void analyze(String[] arguments, Analyzer analyzer) throws IOException {
        for (String argument : arguments) {
            try (Stream<Path> paths = Files.walk(Path.of(argument))) {
                for (Path source : paths.filter(Files::isRegularFile).toList()) {
                    analyzer.analyze(source);
                }
            }
        }
    }

    private static void printUsage(PrintStream output) {
        output.println("Usage: axl-analyzer [--help] <path>...");
        output.println("Analyze Java files or recursively analyze directories.");
    }
}
