package pl.net.brach;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Currency {

    private static final String HEADER_CURRENCY_NAME = "waluta";

    private static final String CURRENCIES_FILE_NAME = "waluty.csv";

    private final List<String> currenciesFromFile = new ArrayList<>();

    public Currency() {
            FileReader currenciesFileReader = validateRatesFileExists();
            if (currenciesFileReader != null) {
                currenciesFromFileFileContents(currenciesFileReader);
                validateCurrenciesFileStructure();
                validateCurrenciesFileCurrencyFormat();
                removeHeaderFromCurrencies();
                removeBadCurrencies();
            } else {
                System.out.println("Program nie będzie działał prawidłowo");
            }
    }

    public List<String> getCurrencies() {
        return currenciesFromFile;
    }

    private FileReader validateRatesFileExists() {
        File currenciesFile = null;
        try {
            currenciesFile = new File(new File(getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath())
                    .getParent() + "\\" + CURRENCIES_FILE_NAME);
        } catch (URISyntaxException e) {
            System.out.println("Ścieżka wskazująca plik " + CURRENCIES_FILE_NAME + " jest wadliwa. " +
                    "Czy plik " + CURRENCIES_FILE_NAME + " istnieje?");
        }

        FileReader fileReader = null;
        try {
            assert currenciesFile != null;
            fileReader = new FileReader(currenciesFile);
        } catch (FileNotFoundException e) {
            System.out.println("Nie znaleziono pliku " + CURRENCIES_FILE_NAME);
        }

        return fileReader;
    }

    private void currenciesFromFileFileContents(FileReader fileReader) {
        BufferedReader br = new BufferedReader(fileReader);
        String line;
        while (true) {
            try {
                if ((line = br.readLine()) != null) {
                    currenciesFromFile.add(line.trim());
                } else {
                    break;
                }
            } catch (IOException e) {
                System.out.println("Wystąpił błąd podczas odczytywania pliku " + CURRENCIES_FILE_NAME);
            }
        }
    }

    private void validateCurrenciesFileStructure() {
        if (!currenciesFromFile.get(0).contains(HEADER_CURRENCY_NAME)) {
            System.out.printf("Plik " + CURRENCIES_FILE_NAME + " ma nieprawidłową strukturę. " +
                            "Spodziewany nagłówek: %1$s ." +
                            "Odczytany nagłówek: " + currenciesFromFile.get(0) + "%n", HEADER_CURRENCY_NAME);
        }
    }

    private void validateCurrenciesFileCurrencyFormat() {
        for (int i = 1; i < currenciesFromFile.size(); i++) { //Start from int = 1 since 0 is a header
            String currencyFromFile = currenciesFromFile.get(i);
            if (currencyFromFile != null && !currencyFromFile.equals("")) {
                if (currencyFromFile.length() != 3) {
                    System.out.println("Waluta ma niepoprawny format. Odczytana waluta: " + currencyFromFile);
                }
            }
        }
    }

    private void removeHeaderFromCurrencies() {
        currenciesFromFile.remove(0);
    }

    private void removeBadCurrencies() {
        for (int i = 0; i < currenciesFromFile.size(); i++) {
            String currencyFromFile = currenciesFromFile.get(i);
            if (currencyFromFile.length() != 3) {
                currenciesFromFile.remove(i--);
            }
        }
    }
}
