package com.example;

public class PaymentClient {
    public boolean ping() {
        return true;
    }
}

@interface JsonProperty {
    String value();
}

class OpenAIConfig {
    @JsonProperty("api_key")
    private String apiKey;
    private String model;
    private String baseUrl;
}
