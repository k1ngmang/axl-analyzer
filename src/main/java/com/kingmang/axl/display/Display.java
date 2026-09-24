package com.kingmang.axl.display;

import com.kingmang.axl.problem.Problem;
import com.kingmang.axl.problem.ProblemList;

//TODO: a simple sketch, then make it look good
public class Display {
    public static void print(){
        for(Problem p : ProblemList.getList()){
            System.out.println(p);
        }
    }
}
