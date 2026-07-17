class NativeNavigation {
    String render() {
        // CASE 4 - Command/Ctrl-click formatValue: normal Java navigation should reach the method below.
        return formatValue();
    }

    private String formatValue() {
        return "native-navigation-ok";
    }
}
