<img src="logo/logo.png" width="600">

# About
Static analyzer for Java and Axolotl languages

## Usage

Build and run the analyzer with one or more Java files or directories:

```shell
mvn package
mvn exec:java -Dexec.mainClass=com.kingmang.axl.Main -Dexec.args="src/main/java"
```

Directories are traversed recursively. The process exits with code `1` when at least
one warning or error is reported, and with code `2` for invalid command-line input
or unreadable sources. Run with `--help` to print the command-line syntax.
