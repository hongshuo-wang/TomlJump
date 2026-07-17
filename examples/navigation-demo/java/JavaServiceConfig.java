@interface JsonProperty {
    String value();
}

class JavaServiceConfig {
    @JsonProperty("java_token")
    private String javaToken;

    private String schema;
}
