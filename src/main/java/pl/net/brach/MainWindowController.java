package pl.net.brach;

import java.awt.print.PrinterJob;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class MainWindowController implements Initializable {

    private static final List<String> DATA_FORMATS = Arrays.asList("dd-MM-yyyy", "dd/MM/yyyy", "ddMMyyyy", "dd.MM.yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy.MM.dd");

    private static final String NBP_API_LINK = "http://api.nbp.pl/api/exchangerates/rates/a/";
    private static final int NBP_API_RETRY_COUNT = 30;
    private static final DateTimeFormatter NBP_API_DATA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    @FXML
    private Button bClose;
    @FXML
    private RadioButton rbPrint;
    @FXML
    private RadioButton rbVAT;
    @FXML
    private TextField tbTransactionAmount;
    @FXML
    private DatePicker dpTransactionDate;
    @FXML
    private ComboBox<String> cbCurrencies;
    @FXML
    private ComboBox<String> cbVAT;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addCurrenciesToComboBox();
        addVATRatesToComboBox();

        dpTransactionDate.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent keyEvent) -> {
            if (keyEvent.getCode() == KeyCode.DOWN) {
                dpTransactionDate.setValue(dpTransactionDate.getValue().minusDays(1));
                keyEvent.consume();
            }
            if (keyEvent.getCode() == KeyCode.UP) {
                dpTransactionDate.setValue(dpTransactionDate.getValue().plusDays(1));
                keyEvent.consume();
            }
        });

        tbTransactionAmount.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*,")) {
                tbTransactionAmount.setText(newValue.replaceAll("[^\\d,]", ""));
            }
        });

        dpTransactionDate.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(".{11}")) {
                dpTransactionDate.getEditor().setText(newValue.replaceAll("[^0-9,-\\/]{10}", ""));
            } else {
                dpTransactionDate.getEditor().setText("");
            }
        });

        modifyDatePickers();
    }

    private void modifyDatePickers() {
        dpTransactionDate.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    for (String pattern : DATA_FORMATS) {
                        try {
                            if (date.isAfter(LocalDate.now())) {
                                return DateTimeFormatter.ofPattern(pattern).format(LocalDate.now());
                            } else {
                                return DateTimeFormatter.ofPattern(pattern).format(date);
                            }
                        } catch (DateTimeException dte) {
                            System.out.println("Format Error");
                        }
                    }
                }
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    for (String pattern : DATA_FORMATS) {
                        try {
                            return LocalDate.parse(string, DateTimeFormatter.ofPattern(pattern));
                        } catch (DateTimeParseException ignored) {
                        }
                    }
                    System.out.println("Parse Error");
                }
                return null;
            }
        });
    }

    private void addCurrenciesToComboBox() {
        Currency currency = new Currency();

        cbCurrencies.getItems().clear();
        cbCurrencies.getItems().addAll(currency.getCurrencies());
        cbCurrencies.getSelectionModel().selectFirst();
    }

    private void addVATRatesToComboBox() {
        VAT vat = new VAT();

        cbVAT.getItems().clear();
        cbVAT.getItems().addAll(vat.getVatRates());
        cbVAT.getSelectionModel().selectFirst();
    }

    @FXML
    private void currencyChosen() { cbCurrencies.getSelectionModel().select(cbCurrencies.getSelectionModel().getSelectedItem()); }

    @FXML
    private void vatRateChosen() {
        cbVAT.getSelectionModel().select(cbVAT.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void okClicked() throws IOException {
        //Get user input data
        if (dpTransactionDate.getEditor().getText().isEmpty()) {
            System.out.println("No data was provided. Aborting...");
        } else {
            LocalDate dTransactionDate = extractTransactionDate();

            String fetchedTransactionData = getData(dTransactionDate, cbCurrencies.getValue());

            String transactionExchangeRatesTableNumber = getTableNumber(fetchedTransactionData);
            String transactionExchangeRatesTableDate = gateDate(fetchedTransactionData);
            String transactionExchangeRatesTransactionRate = getRate(fetchedTransactionData);
            double calculatedTransactionNetValue = calculateNetAmount(transactionExchangeRatesTransactionRate);

            String[] params;

            DecimalFormat format = new DecimalFormat("###,##0.00");

            if (rbVAT.isSelected()) {
                double calculatedTransactionVatValue = calculateVatAmount(transactionExchangeRatesTransactionRate);

                params = new String[6];
                params[5] = format.format(calculatedTransactionVatValue) + " zł";
            } else {
                params = new String[5];
            }

            params[0] = format.format(Double.parseDouble(tbTransactionAmount.getText().replace(",", ".")))
                    + " " + cbCurrencies.getValue();
            params[1] = transactionExchangeRatesTableNumber;
            params[2] = transactionExchangeRatesTableDate;
            params[3] = transactionExchangeRatesTransactionRate.replace(".", ",");
            params[4] = format.format(calculatedTransactionNetValue) + " zł";

            if (rbPrint.isSelected()) {
                ArrayList<String> labelText = generateLabel(params);
                printLabel(labelText);
            }

            ExchangeRates.displaySummary(params);
        }
    }

    private LocalDate extractTransactionDate() {
        LocalDate dTransactionDate = null;
        for (String pattern : DATA_FORMATS) {
            try {
                dTransactionDate = LocalDate.parse(dpTransactionDate.getEditor().getText(), DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) { }
        }
        return dTransactionDate;
    }

    private String getData(LocalDate dData, String sCurrencyInput) throws IOException {
        String dataFetched = "";

        boolean retry = true;

        int loopCount = 0;

        while (retry) {
            //Get the data from NBP API
            try {
                dData = dData.minusDays(1);
                String formattedDate = dData.format(NBP_API_DATA_FORMAT);

                URL nbpApiUrl = new URL(NBP_API_LINK + sCurrencyInput + "/" + formattedDate + "/?format=json");

                try (BufferedReader in = new BufferedReader(new InputStreamReader(nbpApiUrl.openStream()))) {
                    dataFetched = in.readLine();
                }

                return dataFetched;
            } catch (FileNotFoundException ex) {
                if (loopCount > NBP_API_RETRY_COUNT) {
                    System.out.println("Retry count exceeded");
                    retry = false;
                }
                loopCount++;
            }
        }
        return dataFetched;
    }

    private static String getTableNumber(String sData) {
        //VERY dirty workaround for NOKs ONLY. That whole stuff (fetching data, etc.) should be rebuilt to handle JSONs properly
        if (sData.indexOf("no") == 32) {
            return sData.substring(sData.indexOf("no") + 39, sData.indexOf("[") + 22);
        } else {
            return sData.substring(sData.indexOf("no") + 5, sData.indexOf("[") + 22);
        }
    }

    private static String gateDate(String sData) {
        return sData.substring(sData.indexOf("effectiveDate") + 16, sData.indexOf("[") + 51);
    }

    private String getRate(String sData) {
        //Check if the currency is not in 1/100 PLN and adjust if necessary
        String sRate = sData.substring(sData.indexOf("mid") + 5, sData.indexOf("[") + 67);
        if (sRate.charAt(sRate.length() - 1) == ']') {
            sRate = sRate.substring(0, sRate.length() - 1);
        }
        if (sRate.charAt(sRate.length() - 1) == '}') {
            sRate = sRate.substring(0, sRate.length() - 1);
        }
        return sRate;
    }

    private double calculateNetAmount(String sRate) {
        return Math.round((Double.parseDouble(tbTransactionAmount.getText().replace(",", "."))
                * Double.parseDouble(sRate)) * 100.00) / 100.00;
    }

    private double calculateVatAmount(String sRate) {
        return Math.round(
                Math.round(((Double.parseDouble(tbTransactionAmount.getText().replace(",", "."))
                * Double.parseDouble(cbVAT.getValue().replace("%", "")) / 100) * 100.00)) / 100.00
                * Double.parseDouble(sRate) * 100.00) / 100.00;
    }

    private ArrayList<String> generateLabel(String[] args) {
        ArrayList<String> stringArrayList = new ArrayList<>();

        stringArrayList.add(" --------------------------");
        stringArrayList.add("   Kurs 1 " + cbCurrencies.getValue() + " = " + args[3]);
        stringArrayList.add("  wg tab.: " + args[1]);
        stringArrayList.add("     z dn. " + args[2]);
        stringArrayList.add(" --------------------------");
        stringArrayList.add(" " + args[0] + " * " + args[3]);
        stringArrayList.add(" = " + args[4]);

        if (rbVAT.isSelected()) {
            stringArrayList.add(" --------------------------");
            stringArrayList.add(" VAT " + cbVAT.getValue() + " = " + args[5]);
            stringArrayList.add(" --------------------------");
        } else {
            stringArrayList.add(" --------------------------");
            stringArrayList.add(" ");
            stringArrayList.add(" ");
        }

        return stringArrayList;
    }

    private static PrintService getPrintService(String printerName) {
        PrintService printService = null;
        PrintService[] printServices = PrinterJob.lookupPrintServices();

        for (PrintService service : printServices) {
            if (service.getName().equals(printerName)) {
                printService = service;
            }
        }
        return printService;
    }

    private void printLabel(ArrayList<String> stringArrayListToPrint) {
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(OrientationRequested.PORTRAIT);
        pras.add(new MediaPrintableArea(0, 0, LabelPrint.PRINT_PAGE_HEIGHT, LabelPrint.PRINT_PAGE_WIDTH, MediaPrintableArea.MM));
        pras.add(new JobName(ExchangeRates.BRACHSOFT_TITLE + " - Dokument", null));

        PrinterJob printerJob = PrinterJob.getPrinterJob();
        String printServiceName = printerJob.getPrintService().getName();
        boolean isSetPrintServiceSuccess = false;

        try {
            System.out.println("Trying to set print service to: " + ExchangeRates.PRIMARY_PRINTER_NAME);
            printerJob.setPrintService(getPrintService(ExchangeRates.PRIMARY_PRINTER_NAME));
            isSetPrintServiceSuccess = true;
            System.out.println("Successfully set print service to: " + ExchangeRates.PRIMARY_PRINTER_NAME);
        } catch (Exception ex) {
            System.out.println("Failed to set print service to: " + ExchangeRates.PRIMARY_PRINTER_NAME);
        }

        try {
            System.out.println("Trying to set print service to: " + ExchangeRates.SECONDARY_PRINTER_NAME);
            printerJob.setPrintService(getPrintService(ExchangeRates.SECONDARY_PRINTER_NAME));
            isSetPrintServiceSuccess = true;
            System.out.println("Successfully set print service to: " + ExchangeRates.SECONDARY_PRINTER_NAME);
        } catch (Exception ex) {
            System.out.println("Failed to set print service to: " + ExchangeRates.SECONDARY_PRINTER_NAME);
        }

        try {
            if (isSetPrintServiceSuccess) {
                printerJob.setPrintable(new LabelPrint(stringArrayListToPrint));
                printerJob.print(pras);
                System.out.println("Label sent to printer: " + printServiceName);
            } else {
                System.out.println("Failed to print label on: " + printServiceName);
            }
        } catch (Exception ex) {
            System.out.println(ex);
            System.out.println("Failed to print label on: " + printServiceName);
        }
    }

    @FXML
    private void closeClicked() {
        Stage stage = (Stage) bClose.getScene().getWindow();
        stage.close();
    }
}
