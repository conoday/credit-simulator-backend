package com.nusantech.creditsimulator;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestSuite {
    private int passed;

    public static void main(String[] args) throws Exception {
        TestSuite suite = new TestSuite();
        suite.workbookScenario();
        suite.rates();
        suite.validation();
        suite.inputFormats();
        suite.sheetPersistence();
        suite.remoteLoading();
        System.out.println("Tests passed: " + suite.passed);
    }

    private void workbookScenario() {
        LoanInput input = new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 2024,
                money("100000000"), 3, money("25000000"));
        CalculationResult result = new CreditCalculator().calculate(input, 2026);
        equalMoney("2250000.00", result.installments().get(0).monthlyPayment());
        equalMoney("2432250.00", result.installments().get(1).monthlyPayment());
        equalMoney("2641423.50", result.installments().get(2).monthlyPayment());
        equalMoney("54000000.00", result.installments().get(0).closingBalance());
        equalMoney("29187000.00", result.installments().get(1).closingBalance());
        equalMoney("0.00", result.installments().get(2).closingBalance());
        passed++;
    }

    private void rates() {
        CreditCalculator calculator = new CreditCalculator();
        String[] mobil = {"0.08", "0.081", "0.086", "0.087", "0.092", "0.093"};
        String[] motor = {"0.09", "0.091", "0.096", "0.097", "0.102", "0.103"};
        for (int year = 1; year <= 6; year++) {
            equalMoney(mobil[year - 1], calculator.rateFor(VehicleType.MOBIL, year));
            equalMoney(motor[year - 1], calculator.rateFor(VehicleType.MOTOR, year));
        }
        passed++;
    }

    private void validation() {
        valid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BARU, 2025,
                money("100000000"), 6, money("35000000")));
        valid(new LoanInput(VehicleType.MOTOR, VehicleCondition.BEKAS, 2010,
                money("100000000"), 1, money("25000000")));
        valid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BARU, 2026,
                money("100000000"), 1, money("100000000")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BARU, 2024,
                money("100000000"), 3, money("35000000")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BARU, 2025,
                money("100000000"), 3, money("34999999")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 2024,
                money("100000000"), 3, money("24999999")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 2024,
                money("100000000"), 7, money("25000000")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 2024,
                money("1000000001"), 3, money("250000001")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 999,
                money("100000000"), 3, money("25000000")));
        invalid(new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 2024,
                money("100000000"), 3, money("100000001")));
        passed++;
    }

    private void inputFormats() throws IOException {
        Path dir = Files.createTempDirectory("credit-input-test");
        String json = """
                {"vehicleType":"Mobil","vehicleCondition":"Bekas","vehicleYear":2024,
                 "totalLoanAmount":100000000,"loanTenure":3,"downPayment":25000000}
                """;
        Path jsonFile = dir.resolve("input.json");
        Files.writeString(jsonFile, json);
        equalMoney("25000000", InputParser.parseFile(jsonFile).downPayment());

        Path keyedFile = dir.resolve("input.txt");
        Files.writeString(keyedFile, "vehicleType=Mobil\nvehicleCondition=Bekas\nvehicleYear=2024\n"
                + "totalLoanAmount=100000000\nloanTenure=3\ndownPayment=25000000\n");
        equalMoney("25000000", InputParser.parseFile(keyedFile).downPayment());

        Path linesFile = dir.resolve("lines.txt");
        Files.writeString(linesFile, "Mobil\nBekas\n2024\n100000000\n3\n25000000\n");
        equalMoney("25000000", InputParser.parseFile(linesFile).downPayment());
        passed++;
    }

    private void sheetPersistence() throws IOException {
        Path path = Files.createTempFile("credit-sheets", ".json");
        LoanInput input = new LoanInput(VehicleType.MOBIL, VehicleCondition.BEKAS, 2024,
                money("100000000"), 3, money("25000000"));
        CreditSheet sheet = new CreditSheet("Contoh", new CreditCalculator().calculate(input, 2026));
        Map<String, CreditSheet> saved = new LinkedHashMap<>();
        saved.put(sheet.name(), sheet);
        SheetRepository repository = new SheetRepository(path);
        repository.save(saved);
        CreditSheet restored = repository.load().get("Contoh");
        equalMoney("2641423.50", restored.result().installments().get(2).monthlyPayment());
        passed++;
    }

    private void remoteLoading() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String payload = """
                {"vehicleType":"Mobil","vehicleCondition":"Baru","vehicleYear":2025,
                 "totalLoanAmount":1000000000,"loanTenure":6,"downPayment":500000000}
                """;
        server.createContext("/success", exchange -> respond(exchange, 200, payload));
        server.createContext("/invalid", exchange -> respond(exchange, 200, "not json"));
        server.createContext("/error", exchange -> respond(exchange, 503, "unavailable"));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, payload);
        });
        server.start();
        try {
            String root = "http://127.0.0.1:" + server.getAddress().getPort();
            RemoteCalculationLoader loader = new RemoteCalculationLoader(HttpClient.newHttpClient(),
                    Duration.ofSeconds(2));
            equalMoney("500000000", loader.load(URI.create(root + "/success")).downPayment());
            throwsApplication(() -> loader.load(URI.create(root + "/invalid")));
            throwsApplication(() -> loader.load(URI.create(root + "/error")));
            RemoteCalculationLoader shortTimeout = new RemoteCalculationLoader(HttpClient.newHttpClient(),
                    Duration.ofMillis(100));
            throwsApplication(() -> shortTimeout.load(URI.create(root + "/slow")));
            passed++;
        } finally {
            server.stop(0);
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void valid(LoanInput input) {
        LoanValidator.validate(input, 2026);
    }

    private static void invalid(LoanInput input) {
        throwsApplication(() -> LoanValidator.validate(input, 2026));
    }

    private static void throwsApplication(Runnable operation) {
        try {
            operation.run();
        } catch (ApplicationException expected) {
            return;
        }
        throw new AssertionError("Expected ApplicationException");
    }

    private static void equalMoney(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
