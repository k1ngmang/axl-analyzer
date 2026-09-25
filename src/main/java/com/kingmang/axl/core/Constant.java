package com.kingmang.axl.core;

public final class Constant {
    // rule_id
    public static final String CLASS_LINE_ID = "class-lines";
    public static final String BOOLEAN_NAME_ID = "boolean-name";
    public static final String EMPTY_CATCH_ID = "empty-catch-block";
    public static final String CYCLOMATIC_COMPLEX_ID = "cyclomatic-complexity";
    public static final String PARSER_RULE_ID = "parser";

    // analyzer rules
    public static final int DEFAULT_CLASS_INFO_THRESHOLD = 300;
    public static final int DEFAULT_CLASS_WARNING_THRESHOLD = 500;

    public static final int DEFAULT_METHOD_INFO_THRESHOLD = 30;
    public static final int DEFAULT_METHOD_WARNING_THRESHOLD = 150;

    public static final int DEFAULT_CYCLOMATIC_COMPLEXITY_INFO_THRESHOLD = 10;
    public static final int DEFAULT_CYCLOMATIC_COMPLEXITY_WARNING_THRESHOLD = 20;

    private Constant() {
    }
}
