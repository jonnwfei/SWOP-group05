package base.domain.events;

public record ErrorEvent(int lowerBound, int upperBound) implements GameEvent<Integer> {

    @Override
    public Class<Integer> getInputType() {
        return null;
    }

    @Override
    public boolean isValid(Integer input) {
        return lowerBound <= input && upperBound >= input;
    }

    @Override
    public boolean needsInput() {
        return false;
    }
}
