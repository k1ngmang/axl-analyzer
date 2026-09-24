package com.kingmang.axl.core;

import java.nio.file.Path;
import java.util.Objects;

public final class SourceFile {
    private final Path path;
    private final String text;

    public SourceFile(Path path, String text) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.text = Objects.requireNonNull(text, "text");
    }

    public Path getPath() {
        return path;
    }

    public String getText() {
        return text;
    }
}
