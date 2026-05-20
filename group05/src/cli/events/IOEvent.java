package cli.events;

public sealed interface IOEvent permits BidEvents, CountEvents, MenuEvents, MessageIOEvent, PlayEvents, ReportEvents.DisplayReportIOEvent, ReportEvents.ReportFormatSelectionIOEvent, ReportEvents.ReportSavedIOEvent {
    boolean needsInput();
}