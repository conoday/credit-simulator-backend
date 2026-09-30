package com.nusantech.creditsimulator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class RemoteCalculationLoader {
    public static final URI DEFAULT_ENDPOINT = URI.create(
            "https://run.mocky.io/v3/9108b1da-beec-409e-ae14-e8091955666c");

    private final HttpClient client;
    private final Duration timeout;

    public RemoteCalculationLoader() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), Duration.ofSeconds(10));
    }

    public RemoteCalculationLoader(HttpClient client, Duration timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    public LoanInput load(URI endpoint) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Accept", "application/json")
                .GET()
                .build();
        final HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new ApplicationException("Tidak dapat mengakses web service: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Request web service dihentikan.", exception);
        }
        if (response.statusCode() != 200) {
            throw new ApplicationException("Web service mengembalikan HTTP " + response.statusCode() + ".");
        }
        try {
            return InputParser.fromMap(SimpleJson.parseObject(response.body()));
        } catch (ApplicationException exception) {
            throw new ApplicationException("Respons web service tidak valid: " + exception.getMessage(), exception);
        }
    }
}
