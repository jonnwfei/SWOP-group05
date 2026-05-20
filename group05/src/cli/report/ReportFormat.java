package cli.report;

public enum ReportFormat {
    TEXT, MARKDOWN, JSON;

    public String extension() {
        return switch (this) {
            case TEXT     -> "txt";
            case MARKDOWN -> "md";
            case JSON     -> "json";
        };
    }
}