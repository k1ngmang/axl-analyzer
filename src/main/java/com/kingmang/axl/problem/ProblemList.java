package com.kingmang.axl.problem;

import java.util.ArrayList;
import java.util.List;

public class ProblemList {
    private static final List<Problem> list = new ArrayList<>();

    public static void addWarn(String message){
        list.add(new Problem(message, ProblemType.WARNING));
    }

    public static void addInfo(String message){
        list.add(new Problem(message, ProblemType.INFO));
    }

    public static void addErr(String message){
        list.add(new Problem(message, ProblemType.ERROR));
    }

    public static List<Problem> getList(){
        return list;
    }
}
