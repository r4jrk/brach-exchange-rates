package pl.net.brach;


import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.print.PrinterException;
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
import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import javafx.stage.Stage;

import javafx.util.StringConverter;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class MainWindowController implements Initializable {

    private static final String BRACHSOFT_SUMMARY_TITLE = ExchangeRates.BRACHSOFT_TITLE + " - Podsumowanie";

    private static final List<String> DATA_FORMATS = Arrays.asList("dd-MM-yyyy", "dd/MM/yyyy", "ddMMyyyy", "dd.MM.yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy.MM.dd");
    private static final String LABEL_FILE_PATH = "C:/Temp/Kursy.txt";
    private static final String SUMMARY_POPUP_FILE_NAME = "Summary.fxml";

    private static final String NBP_API_LINK = "http://api.nbp.pl/api/exchangerates/rates/a/";
    private static final int NBP_API_RETRY_COUNT = 30;
    private static final DateTimeFormatter NBP_API_DATA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    //FXML fields
    public Button bOK;
    public Button bClose;
    public RadioButton rbPrint;
    public RadioButton rbVAT;
    public TextField tbTransactionAmount;
    public DatePicker dpTransactionDate;
    public ComboBox<String> cbCurrencies;
    public ComboBox<String> cbVAT;
    public Label copyrightLabel;

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
        cbCurrencies.getItems().clear();
        cbCurrencies.getItems().addAll(ExchangeRates.AVAILABLE_CURRENCIES);
        cbCurrencies.getSelectionModel().selectFirst();
    }

    private void addVATRatesToComboBox() {
        cbVAT.getItems().clear();
        cbVAT.getItems().addAll(ExchangeRates.AVAILABLE_VAT_RATES);
        cbVAT.getSelectionModel().selectFirst();
    }

    @FXML
    private void currencyChosen() {
        cbCurrencies.getSelectionModel().select(cbCurrencies.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void vatRateChosen() {
        cbVAT.getSelectionModel().select(cbVAT.getSelectionModel().getSelectedItem());
    }

    private LocalDate extractTransactionDate() {
        LocalDate dTransactionDate = null;
        for (String pattern : DATA_FORMATS) {
            try {
                dTransactionDate = LocalDate.parse(dpTransactionDate.getEditor().getText(), DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        return dTransactionDate;
    }

    @FXML
    private void okClicked() throws IOException, PrinterException {
        //Get user input data
        if (dpTransactionDate.getEditor().getText().equals("")) {
            System.out.println("No data was provided. Aborting...");
        } else {
            LocalDate dTransactionDate = extractTransactionDate();

            String fetchedTransactionData = getData(dTransactionDate, cbCurrencies.getValue());

            String transactionExchangeRatesTableNumber = getTableNumber(fetchedTransactionData);
            String transactionExchangeRatesTableDate = getDay(fetchedTransactionData);
            String transactionExchangeRatesTransactionRate = getRate(fetchedTransactionData);
            double calculatedTransactionValue = calculateAmount(transactionExchangeRatesTransactionRate);

            String[] parameters;
            double vat;
            DecimalFormat format = new DecimalFormat("###,##0.00");

            if (rbVAT.isSelected()) {
                vat = calculateVAT(calculatedTransactionValue);
                parameters = new String[6];
                parameters[5] = format.format(vat) + " zł";
            } else {
                parameters = new String[5];
            }

            parameters[0] = format.format(Double.parseDouble(tbTransactionAmount.getText().replace(",", ".")))
                    + " " + cbCurrencies.getValue();
            parameters[1] = transactionExchangeRatesTableNumber;
            parameters[2] = transactionExchangeRatesTableDate;
            parameters[3] = transactionExchangeRatesTransactionRate.replace(".", ",");
            parameters[4] = format.format(calculatedTransactionValue) + " zł";

            if (rbPrint.isSelected()) {
                ArrayList<String> labelText = generateLabel(parameters);
                //saveToLabelFile(labelText);
                printLabel(labelText);
            }

            showSummary(parameters);
        }
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

    private static String getTableNumber(String sDane) {
        return sDane.substring(sDane.indexOf("no") + 5, sDane.indexOf("[") + 22);
    }

    private static String getDay(String sDane) {
        return sDane.substring(sDane.indexOf("effectiveDate") + 16, sDane.indexOf("[") + 51);
    }

    private String getRate(String sDane) {
        //Check if the currency is not in 1/100 PLN and adjust if necessary
        String sRate = sDane.substring(sDane.indexOf("mid") + 5, sDane.indexOf("[") + 67);
        if (sRate.charAt(sRate.length() - 1) == ']') {
            sRate = sRate.substring(0, sRate.length() - 1);
        }
        if (sRate.charAt(sRate.length() - 1) == '}') {
            sRate = sRate.substring(0, sRate.length() - 1);
        }
        return sRate;
    }

    private double calculateAmount(String sRate) {
        return Math.round((Double.parseDouble(tbTransactionAmount.getText().replace(",", "."))
                * Double.parseDouble(sRate)) * 100.00) / 100.00;
    }

    private double calculateVAT(double calculatedTransactionValue) {
        return (Double.parseDouble(cbVAT.getValue().replace("%", ""))
                * calculatedTransactionValue) / 100;
    }

    private void showSummary(String[] params) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource(SUMMARY_POPUP_FILE_NAME));
        Pane root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(ExchangeRates.STYLE_PATH);

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.setTitle(BRACHSOFT_SUMMARY_TITLE);
        stage.getIcons().add(new Image(ExchangeRates.ICON_PATH));

        SummaryController controller = fxmlLoader.getController();
        controller.generateSummary(params);

        stage.setResizable(false);
        stage.show();
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

    private void printLabel(ArrayList<String> stringArrayListToPrint) throws PrinterException {
        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
        pras.add(OrientationRequested.PORTRAIT);
        pras.add(new MediaPrintableArea(0, 0, LabelPrint.PRINT_PAGE_HEIGHT, LabelPrint.PRINT_PAGE_WIDTH, MediaPrintableArea.MM));
        pras.add(new JobName(ExchangeRates.BRACHSOFT_TITLE + " - Dokument", null));

        PrinterJob printerJob = PrinterJob.getPrinterJob();

        if (!printerJob.getPrintService().getName().equals(ExchangeRates.PRINTER_NAME)) {
            printerJob.setPrintService(getPrintService(ExchangeRates.PRINTER_NAME));
        }

        printerJob.setPrintable(new LabelPrint(stringArrayListToPrint));
        printerJob.print(pras);
    }

    private void saveToLabelFile(String[] labelText) throws IOException {
        File tempFile = new File(LABEL_FILE_PATH);

        boolean directoryCreated = false;

        File directoryFile = tempFile.getParentFile();

        if (!directoryFile.exists()) {
            directoryCreated = directoryFile.mkdir();
        }

        if (directoryCreated) {
            System.out.println("Directory: " + directoryFile.getAbsolutePath() + " created");
        }

        try (
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String stringToBeSaved : labelText) {
                writer.write(stringToBeSaved + System.lineSeparator());
            }
        }
    }

    private String readFromLabelFile() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(LABEL_FILE_PATH));
        StringBuilder stringBuilder = new StringBuilder();

        String textLine = bufferedReader.readLine();

        while (textLine != null) {
            stringBuilder.append(textLine);
            stringBuilder.append(System.lineSeparator());
            textLine = bufferedReader.readLine();
        }

        return stringBuilder.toString();
    }

    private static DocPrintJob getPrinterJob(String printerName) {
        DocPrintJob job = null;
        PrintService[] services = PrinterJob.lookupPrintServices();

        for (PrintService printService : services) {
            if (printService.getName().equals(printerName)) {
                job = printService.createPrintJob();
            }
        }
        return job;
    }

    @FXML
    private void copyEmail() {
        Tooltip tooltip = new Tooltip();
        tooltip.setText("Skopiowano adres e-mail");
        StringSelection selection = new StringSelection("jurek.rafal@outlook.com");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        copyrightLabel.setTooltip(tooltip);
    }

    @FXML
    private void closeClicked() {
        Stage stage = (Stage) bClose.getScene().getWindow();
        stage.close();
    }
}
