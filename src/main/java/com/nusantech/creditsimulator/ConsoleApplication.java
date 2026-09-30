package com.nusantech.creditsimulator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public final class ConsoleApplication {
    private final CreditCalculator calculator;
    private final RemoteCalculationLoader remoteLoader;
    private final SheetRepository repository;
    private final Map<String, CreditSheet> sheets;
    private final Scanner scanner;
    private String activeSheetName;
    private CalculationResult activeResult;

    public ConsoleApplication() {
        this(new CreditCalculator(), new RemoteCalculationLoader(),
                new SheetRepository(Paths.get("sheets.json")), new Scanner(System.in));
    }

    ConsoleApplication(CreditCalculator calculator, RemoteCalculationLoader remoteLoader,
                       SheetRepository repository, Scanner scanner) {
        this.calculator = calculator;
        this.remoteLoader = remoteLoader;
        this.repository = repository;
        this.scanner = scanner;
        this.sheets = new LinkedHashMap<>();
        try {
            this.sheets.putAll(repository.load());
        } catch (ApplicationException exception) {
            System.err.println("Peringatan: " + exception.getMessage());
        }
    }

    public void run() {
        System.out.println("Credit Simulator");
        System.out.println("Ketik 'show' untuk melihat command.");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String command = scanner.nextLine().trim();
            if (command.isEmpty()) {
                continue;
            }
            try {
                if (execute(command)) {
                    return;
                }
            } catch (ApplicationException exception) {
                System.out.println("Error: " + exception.getMessage());
            } catch (RuntimeException exception) {
                System.out.println("Error: operasi gagal dijalankan.");
            }
        }
    }

    private boolean execute(String rawCommand) {
        String normalized = rawCommand.toLowerCase();
        if (normalized.equals("show") || normalized.equals("help")) {
            showCommands();
        } else if (normalized.equals("new")) {
            createNewCalculation();
        } else if (normalized.equals("load")) {
            loadRemoteCalculation();
        } else if (normalized.equals("save sheet")) {
            saveActiveSheet();
        } else if (normalized.equals("list sheets") || normalized.equals("sheets")) {
            listSheets();
        } else if (normalized.startsWith("switch sheet ")) {
            switchSheet(rawCommand.substring("switch sheet ".length()).trim());
        } else if (normalized.equals("exit") || normalized.equals("quit")) {
            return true;
        } else {
            System.out.println("Command tidak dikenal. Ketik 'show'.");
        }
        return false;
    }

    private void showCommands() {
        System.out.println("Commands:");
        System.out.println("  show                         tampilkan command");
        System.out.println("  new                          buat kalkulasi dari input console");
        System.out.println("  load                         load JSON dari web service");
        System.out.println("  save sheet                   simpan kalkulasi aktif ke sheets.json");
        System.out.println("  list sheets                  tampilkan sheet yang tersimpan");
        System.out.println("  switch sheet <nama>          aktifkan sheet berdasarkan nama");
        System.out.println("  exit                         keluar");
    }

    private void createNewCalculation() {
        LoanInput input = InteractiveInput.readLoan(scanner);
        activeResult = calculator.calculate(input, Year.now().getValue());
        activeSheetName = null;
        ResultPrinter.print(activeResult, null);
    }

    private void loadRemoteCalculation() {
        System.out.println("Loading: " + RemoteCalculationLoader.DEFAULT_ENDPOINT);
        LoanInput input = remoteLoader.load(RemoteCalculationLoader.DEFAULT_ENDPOINT);
        activeResult = calculator.calculate(input, Year.now().getValue());
        activeSheetName = null;
        ResultPrinter.print(activeResult, null);
    }

    private void saveActiveSheet() {
        if (activeResult == null) {
            throw new ApplicationException("Belum ada kalkulasi aktif.");
        }
        System.out.print("Nama sheet (kosong untuk nama otomatis): ");
        if (!scanner.hasNextLine()) {
            throw new ApplicationException("Input dihentikan sebelum nama sheet diisi.");
        }
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            name = "sheet-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .format(LocalDateTime.now());
        }
        CreditSheet sheet = new CreditSheet(name, activeResult);
        sheets.put(name, sheet);
        repository.save(sheets);
        activeSheetName = name;
        System.out.println("Sheet tersimpan: " + name);
    }

    private void listSheets() {
        if (sheets.isEmpty()) {
            System.out.println("Belum ada sheet tersimpan.");
            return;
        }
        for (String name : sheets.keySet()) {
            String marker = name.equals(activeSheetName) ? " *" : "";
            System.out.println("- " + name + marker);
        }
    }

    private void switchSheet(String name) {
        if (name.isEmpty()) {
            throw new ApplicationException("Nama sheet wajib diisi.");
        }
        CreditSheet sheet = sheets.get(name);
        if (sheet == null) {
            throw new ApplicationException("Sheet tidak ditemukan: " + name);
        }
        activeSheetName = name;
        activeResult = sheet.result();
        ResultPrinter.print(activeResult, activeSheetName);
    }
}
