package cli.elements;

public record Response(String rawInput) {
    public Response {
        if (rawInput == null)
            throw new IllegalArgumentException("Response must not contain null rawInput");
    }
}