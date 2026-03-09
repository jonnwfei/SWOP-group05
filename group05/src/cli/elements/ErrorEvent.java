package cli.elements;

public record ErrorEvent(int lowerBound, int upperBound) implements GameEvent{
    private void RenderErrorEvent(ErrorEvent event) {
        System.out.println("Please give a number between " + event.lowerBound + " and " + event.upperBound);
    }
}
