package com.kingmang.axl.cfg;

import java.util.stream.Collectors;

public final class CfgDotExporter {
    public String export(ControlFlowGraph graph) {
        StringBuilder result = new StringBuilder("digraph cfg {\n");
        result.append("  node [shape=box];\n");
        for (BasicBlock block : graph.blocks()) {
            String instructions = block.instructions().stream()
                    .map(instruction -> escape(oneLine(instruction.source().toString())))
                    .collect(Collectors.joining("\\l"));
            String special = block.id().equals(graph.entry()) ? "ENTRY"
                    : block.id().equals(graph.normalExit()) ? "NORMAL EXIT"
                    : block.id().equals(graph.exceptionalExit()) ? "EXCEPTIONAL EXIT"
                    : "";
            String label = escape(block.id() + (special.isEmpty() ? "" : " " + special))
                    + (instructions.isEmpty() ? "" : "\\l" + instructions + "\\l");
            result.append("  ").append(block.id().value())
                    .append(" [label=\"").append(label).append("\"];\n");
        }
        for (BasicBlock block : graph.blocks()) {
            for (CfgEdge edge : block.outgoingEdges()) {
                String label = edge.kind().name()
                        + edge.getLabel().map(value -> ": " + value).orElse("");
                result.append("  ").append(edge.source().value())
                        .append(" -> ").append(edge.target().value())
                        .append(" [label=\"").append(escape(label)).append("\"];\n");
            }
        }
        return result.append("}\n").toString();
    }

    private static String oneLine(String value) {
        return value.replace('\n', ' ').replace('\r', ' ');
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
