package com.tbg.wms.cli.commands.rail;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailPrintCommandTest {

    @Test
    void commandSupportsNonDestructiveSystemDefaultPrinterValidation() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        RailPrintCommand command = new RailPrintCommand(() -> "System default printer: Office Printer");
        try {
            System.setOut(new PrintStream(stdout, true));
            CommandLine cli = new CommandLine(command);
            cli.setOut(new PrintWriter(stdout, true));

            int exitCode = cli.execute("--validate-system-default-print");

            assertEquals(0, exitCode);
            assertTrue(stdout.toString().contains("System default printer: Office Printer"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
