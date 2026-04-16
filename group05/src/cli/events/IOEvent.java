package cli.events;

public sealed interface IOEvent permits
        BidEvents,
        CountEvents,
        PlayEvents,
        MenuEvents,
        MessageIOEvent {
    boolean needsInput();
}