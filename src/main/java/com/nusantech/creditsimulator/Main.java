package com.nusantech.creditsimulator;

import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.time.Year;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                new ConsoleApplication().run();
                return;
            }
            if (args.length != 1) {
                throw new ApplicationException("Gunakan tanpa argumen atau satu path file input.");
            }
            LoanInput input = InputParser.parseFile(Path.of(args[0]));
            CalculationResult result = new CreditCalculator().calculate(input, Year.now().getValue());
            ResultPrinter.print(result, null);
        } catch (InvalidPathException exception) {
            System.err.println("Error: path file input tidak valid.");
            System.exit(1);
        } catch (ApplicationException exception) {
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }
}
