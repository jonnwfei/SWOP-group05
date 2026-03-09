package cli.elements;

public record OptionsEvent(Enum<?>[] options) {
    private void RenderOptionsEvent(OptionsEvent event) {
        System.out.println("All Options:");
        for (int i = 0; i < options.length; i++) {
            System.out.println("   [" + i + "] " + options[i].name());
        }
    }
}
