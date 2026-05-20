package cli.flows;

import base.GameController;
import cli.TerminalManager;
import cli.events.MessageIOEvent;
import cli.events.ReportEvents.*;
import cli.report.ReportData;
import cli.report.ReportFormat;
import cli.report.ReportService;
import cli.util.TerminalInputHelper;

import java.io.IOException;
import java.nio.file.Path;

public class ReportFlow {

    private final TerminalManager terminalManager;
    private final TerminalInputHelper input;
    private final GameController controller;
    private final ReportService reportService;

    public ReportFlow(TerminalManager terminalManager, GameController controller) {
        if (terminalManager == null) throw new IllegalArgumentException("terminalManager cannot be null");
        if (controller == null)      throw new IllegalArgumentException("controller cannot be null");

        this.terminalManager = terminalManager;
        this.input           = new TerminalInputHelper(terminalManager);
        this.controller      = controller;
        this.reportService   = new ReportService();
    }

    /**
     * Called when the user decides to quit.
     * Prompts for format, generates report, displays it, and saves it.
     */
    public void run() {
        ReportFormat format = promptFormat();

        ReportData data = reportService.buildReportData(controller);
        String report = reportService.generate(data, format);

        terminalManager.handle(new DisplayReportIOEvent(report));

        try {
            Path saved = reportService.save(report, format);
            terminalManager.handle(new ReportSavedIOEvent(saved.toAbsolutePath()));
        } catch (IOException e) {
            terminalManager.handle(
                    new MessageIOEvent(
                            "Warning: could not save report file (" + e.getMessage() + ")"
                    )
            );
        }
    }

    private ReportFormat promptFormat() {
        int choice = input.askInt(new ReportFormatSelectionIOEvent(), 1, 3);

        return switch (choice) {
            case 1 -> ReportFormat.TEXT;
            case 2 -> ReportFormat.MARKDOWN;
            case 3 -> ReportFormat.JSON;
            default -> throw new IllegalStateException("Unexpected report format choice: " + choice);
        };
    }
}