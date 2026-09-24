package com.kingmang.axl;

import com.kingmang.axl.core.AnalysisRunContext;
import com.kingmang.axl.core.Analyzer;
import com.kingmang.axl.problem.Problem;
import com.kingmang.axl.problem.Severity;
import com.kingmang.axl.report.ConsoleReporter;
import com.kingmang.axl.report.Reporter;
import com.kingmang.axl.rule.ClassLineRule;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class Main {
    private static final int EXIT_OK = 0;
    private static final int EXIT_FINDINGS = 1;
    private static final int EXIT_USAGE_ERROR = 2;

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    public static int run(String[] args, PrintStream output, PrintStream error) {
        if (args.length == 0) {
            error.println("No input paths specified.");
            printUsage(error);
            return EXIT_USAGE_ERROR;
        }

        if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            printUsage(output);
            return EXIT_OK;
        }

        for (String arg : args) {
            if (arg.startsWith("-")) {
                error.println("Unknown option: " + arg);
                printUsage(error);
                return EXIT_USAGE_ERROR;
            }
        }

        List<Path> sourceFiles;
        try {
            sourceFiles = findJavaSources(args);
        } catch (IOException | IllegalArgumentException exception) {
            error.println("Unable to collect sources: " + exception.getMessage());
            return EXIT_USAGE_ERROR;
        }

        AnalysisRunContext runContext = new AnalysisRunContext(List.of(new ClassLineRule()));
        Analyzer analyzer = new Analyzer(runContext);
        for (Path source : sourceFiles) {
            try {
                analyzer.analyze(source);
            } catch (IOException exception) {
                error.println("Unable to read " + source + ": " + exception.getMessage());
                return EXIT_USAGE_ERROR;
            }
        }

        List<Problem> problems = runContext.getCollector().snapshot();
        Reporter reporter = new ConsoleReporter();
        reporter.report(problems, output);

        boolean hasActionableProblems = problems.stream()
                .anyMatch(problem -> problem.getSeverity() == Severity.WARNING
                        || problem.getSeverity() == Severity.ERROR);
        return hasActionableProblems ? EXIT_FINDINGS : EXIT_OK;
    }

    private static List<Path> findJavaSources(String[] arguments) throws IOException {
        Set<Path> sources = new LinkedHashSet<>();
        for (String argument : arguments) {
            Path input = Path.of(argument).toAbsolutePath().normalize();
            if (!Files.exists(input)) {
                throw new IllegalArgumentException("path does not exist: " + input);
            }

            if (Files.isRegularFile(input)) {
                if (isJavaSource(input)) {
                    sources.add(input);
                } else {
                    throw new IllegalArgumentException("not a Java source file: " + input);
                }
                continue;
            }

            if (!Files.isDirectory(input)) {
                throw new IllegalArgumentException("unsupported path: " + input);
            }

            try (Stream<Path> paths = Files.walk(input)) {
                paths.filter(Files::isRegularFile)
                        .filter(Main::isJavaSource)
                        .map(path -> path.toAbsolutePath().normalize())
                        .forEach(sources::add);
            }
        }

        List<Path> sortedSources = new ArrayList<>(sources);
        sortedSources.sort(Comparator.comparing(Path::toString));
        return sortedSources;
    }

    private static boolean isJavaSource(Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private static void printUsage(PrintStream output) {
        output.println("Usage: axl-analyzer [--help] <path>...");
        output.println("Analyze Java files or recursively analyze directories.");
    }
}
