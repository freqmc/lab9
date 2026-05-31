package org.example.lab9;

import java.io.PrintWriter;

@FunctionalInterface
public interface Command {
    void execute(String[] parts, PrintWriter out, SessionContext context);
}