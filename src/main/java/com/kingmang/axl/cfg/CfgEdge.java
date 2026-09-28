package com.kingmang.axl.cfg;

import com.github.javaparser.ast.Node;

import java.util.Objects;
import java.util.Optional;

public record CfgEdge(
        BlockId source,
        BlockId target,
        EdgeKind kind,
        String label,
        Node sourceNode
) {
    public CfgEdge {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(kind, "kind");
    }

    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    public Optional<Node> getSourceNode() {
        return Optional.ofNullable(sourceNode);
    }
}
