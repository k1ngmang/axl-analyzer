package com.kingmang.axl.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ControlFlowGraph {
    private final ExecutableScope scope;
    private final BlockId entry;
    private final BlockId normalExit;
    private final BlockId exceptionalExit;
    private final Map<BlockId, BasicBlock> blocks;
    private final Map<BlockId, List<CfgEdge>> incomingEdges;

    public ControlFlowGraph(
            ExecutableScope scope,
            BlockId entry,
            BlockId normalExit,
            BlockId exceptionalExit,
            Collection<BasicBlock> blocks
    ) {
        this.scope = Objects.requireNonNull(scope, "scope");
        this.entry = Objects.requireNonNull(entry, "entry");
        this.normalExit = Objects.requireNonNull(normalExit, "normalExit");
        this.exceptionalExit = Objects.requireNonNull(exceptionalExit, "exceptionalExit");

        Map<BlockId, BasicBlock> byId = new LinkedHashMap<>();
        for (BasicBlock block : blocks) {
            if (byId.put(block.id(), block) != null) {
                throw new IllegalArgumentException("Duplicate basic block " + block.id());
            }
        }
        this.blocks = Collections.unmodifiableMap(byId);

        Map<BlockId, List<CfgEdge>> incoming = new LinkedHashMap<>();
        byId.keySet().forEach(id -> incoming.put(id, new ArrayList<>()));
        byId.values().stream()
                .flatMap(block -> block.outgoingEdges().stream())
                .forEach(edge -> {
                    List<CfgEdge> edges = incoming.get(edge.target());
                    if (edges == null) {
                        throw new IllegalArgumentException("Edge targets missing block " + edge.target());
                    }
                    edges.add(edge);
                });
        incoming.replaceAll((ignored, edges) -> List.copyOf(edges));
        this.incomingEdges = Collections.unmodifiableMap(incoming);

        requireBlock(entry);
        requireBlock(normalExit);
        requireBlock(exceptionalExit);
    }

    public ExecutableScope scope() {
        return scope;
    }

    public BlockId entry() {
        return entry;
    }

    public BlockId normalExit() {
        return normalExit;
    }

    public BlockId exceptionalExit() {
        return exceptionalExit;
    }

    public Collection<BasicBlock> blocks() {
        return blocks.values();
    }

    public BasicBlock block(BlockId id) {
        return requireBlock(id);
    }

    public List<CfgEdge> successors(BlockId id) {
        return requireBlock(id).outgoingEdges();
    }

    public List<CfgEdge> predecessors(BlockId id) {
        List<CfgEdge> result = incomingEdges.get(id);
        if (result == null) {
            throw new IllegalArgumentException("Unknown basic block " + id);
        }
        return result;
    }

    public Set<BlockId> reachableBlocks() {
        Set<BlockId> reached = new LinkedHashSet<>();
        ArrayDeque<BlockId> work = new ArrayDeque<>();
        work.add(entry);
        while (!work.isEmpty()) {
            BlockId id = work.removeFirst();
            if (!reached.add(id)) {
                continue;
            }
            successors(id).stream().map(CfgEdge::target).forEach(work::addLast);
        }
        return Collections.unmodifiableSet(reached);
    }

    private BasicBlock requireBlock(BlockId id) {
        BasicBlock block = blocks.get(id);
        if (block == null) {
            throw new IllegalArgumentException("Unknown basic block " + id);
        }
        return block;
    }
}
