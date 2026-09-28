package com.kingmang.axl.cfg;

public record BlockId(int value) implements Comparable<BlockId> {
    public BlockId {
        if (value < 0) {
            throw new IllegalArgumentException("Block id must be non-negative");
        }
    }

    @Override
    public int compareTo(BlockId other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return "B" + value;
    }
}
