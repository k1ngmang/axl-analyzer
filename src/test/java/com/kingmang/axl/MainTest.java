package com.kingmang.axl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void helpExitsSuccessfully() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[]{"--help"}, new PrintStream(output), System.err);

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("Usage:"));
    }

    @Test
    void recursivelyFindsJavaFilesAndFailsOnParseErrors() throws IOException {
        Path nested = Files.createDirectories(temporaryDirectory.resolve("nested"));
        Files.writeString(nested.resolve("Broken.java"), "class {");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = Main.run(
                new String[]{temporaryDirectory.toString()},
                new PrintStream(output),
                System.err
        );

        assertEquals(1, exitCode);
        assertTrue(output.toString().contains("[ERROR] parser:"));
    }
}
