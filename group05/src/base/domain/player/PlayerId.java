package base.domain.player;

import java.util.UUID;

public record PlayerId(UUID id) {

    // 1. Compact constructor for validation
    public PlayerId {
        if (id == null) {
            throw new IllegalArgumentException("PlayerId cannot be null");
        }
    }

    // 2. Convenience constructor to auto-generate a new ID
    public PlayerId() {
        this(UUID.randomUUID());
    }

    // 3. Factory method for loading from storage/JSON
    public static PlayerId fromString(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            throw new IllegalArgumentException("String ID cannot be null or empty");
        }
        return new PlayerId(UUID.fromString(idStr));
    }
}