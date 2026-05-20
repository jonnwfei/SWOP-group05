package cli.report;

public interface ReportGenerator {
    /** Render the report as a String in the target format. */
    String generate(ReportData data);
}