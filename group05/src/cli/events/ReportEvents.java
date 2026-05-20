package cli.events;

import java.nio.file.Path;

public class ReportEvents {

    public record ReportFormatSelectionIOEvent() implements IOEvent {
        @Override
        public boolean needsInput() {
            return true;
        }
    }

    public record DisplayReportIOEvent(String report) implements IOEvent {
        @Override
        public boolean needsInput() {
            return false;
        }
    }

    public record ReportSavedIOEvent(Path path) implements IOEvent {
        @Override
        public boolean needsInput() {
            return false;
        }
    }
}