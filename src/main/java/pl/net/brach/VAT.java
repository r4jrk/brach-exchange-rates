package pl.net.brach;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class VAT {

    private static final String HEADER_VAT_RATE_NAME = "stawka vat";

    private static final String VAT_FILE_NAME = "vat.csv";

    private final List<String> vatFromFile = new ArrayList<>();

    public VAT() {
        FileReader vatFileReader = validateVatFileExists();
        if (vatFileReader != null) {
            vatFromFileFileContents(vatFileReader);
            validateVatFileStructure();
            validateVatFileVatFormat();
            removeHeaderFromVat();
            removeBadVatRates();
        } else {
            System.out.println("Program nie będzie działał prawidłowo");
        }
    }

    public List<String> getVatRates() {
        return vatFromFile;
    }

    private FileReader validateVatFileExists() {
        File vatFile = null;
        try {
            vatFile = new File(new File(getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath())
                    .getParent() + "\\" + VAT_FILE_NAME);
        } catch (URISyntaxException e) {
            System.out.println("Ścieżka wskazująca plik " + VAT_FILE_NAME + " jest wadliwa. " +
                    "Czy plik " + VAT_FILE_NAME + " istnieje?");
        }

        FileReader fileReader = null;
        try {
            assert vatFile != null;
            fileReader = new FileReader(vatFile);
        } catch (FileNotFoundException e) {
            System.out.println("Nie znaleziono pliku " + VAT_FILE_NAME);
        }

        return fileReader;
    }

    private void vatFromFileFileContents(FileReader fileReader) {
        BufferedReader br = new BufferedReader(fileReader);
        String line;
        while (true) {
            try {
                if ((line = br.readLine()) != null) {
                    vatFromFile.add(line.trim());
                } else {
                    break;
                }
            } catch (IOException e) {
                System.out.println("Wystąpił błąd podczas odczytywania pliku " + VAT_FILE_NAME);
            }
        }
    }

    private void validateVatFileStructure() {
        if (!vatFromFile.get(0).contains(HEADER_VAT_RATE_NAME)) {
            System.out.printf("Plik " + VAT_FILE_NAME + " ma nieprawidłową strukturę. " +
                    "Spodziewany nagłówek: %1$s." +
                    "Odczytany nagłówek: " + vatFromFile.get(0) + "%n", HEADER_VAT_RATE_NAME);
        }
    }

    private void validateVatFileVatFormat() {
        for (int i = 1; i < vatFromFile.size(); i++) { //Start from int = 1 since 0 is a header
            String vatRateFromFile = vatFromFile.get(i);
            if (vatRateFromFile != null && !vatRateFromFile.equals("")) {
                if (!vatRateFromFile.matches("^\\d{1,2}%")) {
                    System.out.println("Stawka VAT ma niepoprawny format. Odczytana stawka: " + vatRateFromFile +
                            " spodziewany format: #%, gdzie '#' oznacza od jednej do dwóch cyfr");
                }
            }
        }
    }

    private void removeHeaderFromVat() {
        vatFromFile.remove(0);
    }

    private void removeBadVatRates() {
        for (int i = 0; i < vatFromFile.size(); i++) {
            String vatRateFromFile = vatFromFile.get(i);
            if (!vatRateFromFile.matches("^\\d{1,2}%")) {
                vatFromFile.remove(i--);
            }
        }
    }
}