package com.kingmang.axl.report;

import com.kingmang.axl.problem.Problem;

import java.io.PrintStream;
import java.util.List;

public interface Reporter {
    void report(List<Problem> problems, PrintStream output);
}
