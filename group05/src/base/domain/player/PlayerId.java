package base.domain.player;

public record PlayerId(String id) {
    public PlayerId {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("PlayerId cannot be null or empty");
        }
    }
}
