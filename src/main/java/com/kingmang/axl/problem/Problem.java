package com.kingmang.axl.problem;

public class Problem {
    ProblemType type;
    String message;

    public Problem(String message, ProblemType type) {
        this.message = message;
        this.type = type;
    }

    @Override
    public String toString(){
        return "[" + type + "]: " + message;
    }
}
