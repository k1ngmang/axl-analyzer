package com.kingmang.axl.report;

import com.kingmang.axl.problem.Problem;

import java.io.PrintStream;
import java.util.List;

public final class ConsoleReporter implements Reporter {
    @Override
    public void report(List<Problem> problems, PrintStream output) {
        problems.forEach(output::println);
    }
}
