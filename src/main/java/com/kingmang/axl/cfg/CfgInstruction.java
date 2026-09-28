package com.kingmang.axl.cfg;

import com.github.javaparser.ast.Node;

import java.util.Objects;

/** An operation evaluated inside a basic block, tied to its source AST node. */
public record CfgInstruction(Node source, Kind kind) {
    public enum Kind {
        EVALUATE,
        MONITOR_ENTER,
        MONITOR_EXIT,
        RESOURCE_CLOSE,
        CATCH_PARAMETER
    }

    public CfgInstruction {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(kind, "kind");
    }

    public CfgInstruction(Node source) {
        this(source, Kind.EVALUATE);
    }
}
