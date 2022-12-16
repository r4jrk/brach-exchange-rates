package kursy;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.fxml.FXMLLoader;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.text.ParseException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.OrientationRequested;

public class MainWindowController implements Initializable {

    KursyWalut RozniceKursowe = new KursyWalut();
    PodsumowanieController PodsumowanieController = new PodsumowanieController();

    private static final String PODSUMOWANIE_TITLE = "BRACHSoft - Kursy walut v.1.11 - Podsumowanie";
    private static final String NBP_API_LINK = "http://api.nbp.pl/api/exchangerates/rates/a/";
    private static final String FILE_PATH = "C:/Temp/Kursy.txt";
    private static final int RETRY_COUNT = 30;
    private static final DateTimeFormatter API_DATA_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final List<String> DATA_FORMATS = Arrays.asList("dd-MM-yyyy", "dd/MM/yyyy", "ddMMyyyy", "dd.MM.yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy.MM.dd");
    private static final float PAGE_HEIGHT = 60;//85
    private static final float PAGE_WIDTH = 80;//141

    @FXML
    public MainWindowController MainWindow;
    public Button bOK;
    public Button bZamknij;
    public RadioButton rbDrukuj;
    public RadioButton rbVAT;
    public TextField tbWartoscTransakcji;
    public DatePicker dpDataTransakcji;
    public ComboBox<String> cbWaluty;
    public ComboBox<String> cbVAT;
    public Label copyrightLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        addCurrenciesToComboBox();
        addVATRatesToComboBox();

        dpDataTransakcji.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent keyEvent) -> {
            if (keyEvent.getCode() == KeyCode.DOWN) {
                dpDataTransakcji.setValue(dpDataTransakcji.getValue().minusDays(1));
                keyEvent.consume();
            }
            if (keyEvent.getCode() == KeyCode.UP) {
                dpDataTransakcji.setValue(dpDataTransakcji.getValue().plusDays(1));
                keyEvent.consume();
            }
        });

        tbWartoscTransakcji.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("\\d*,")) {
                    tbWartoscTransakcji.setText(newValue.replaceAll("[^\\d,]", ""));
                }
            }
        });

        dpDataTransakcji.getEditor().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches(".{11}")) {
                    dpDataTransakcji.getEditor().setText(newValue.replaceAll("[^0-9,-\\/]{10}", ""));
                } else {
                    dpDataTransakcji.getEditor().setText("");
                }
            }
        });

        modifyDatePickers();
    }

    private void modifyDatePickers() {
        dpDataTransakcji.setConverter(new StringConverter<LocalDate>() {
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
                        } catch (DateTimeParseException dtpe) {
                        }
                    }
                    System.out.println("Parse Error");
                }
                return null;
            }
        });
    }

    private void addCurrenciesToComboBox() {
        cbWaluty.getItems().clear();
        cbWaluty.getItems().addAll("EUR", "USD", "CHF", "GBP", "CZK", "RON", "HUF");
        cbWaluty.getSelectionModel().selectFirst();
    }

    private void addVATRatesToComboBox() {
        cbVAT.getItems().clear();
        cbVAT.getItems().addAll("23%", "8%", "5%");
        cbVAT.getSelectionModel().selectFirst();
    }

    @FXML
    private void walutaWybrana() {
        cbWaluty.getSelectionModel().select(cbWaluty.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void stawkaVATWybrana() {
        cbVAT.getSelectionModel().select(cbVAT.getSelectionModel().getSelectedItem());
    }

    private LocalDate extractDataTransakcji() {
        LocalDate dDataTransakcji = null;
        for (String pattern : DATA_FORMATS) {
            try {
                dDataTransakcji = LocalDate.parse(dpDataTransakcji.getEditor().getText(), DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException dtpe) {
            }
        }
        return dDataTransakcji;
    }

    @FXML
    private void okClick() throws ParseException, MalformedURLException, IOException {
        //Pobierz wprowadzone dane
        LocalDate dDataTransakcji = extractDataTransakcji();

        String pobraneDaneDlaTransakcji = pobierzDane(dDataTransakcji, cbWaluty.getValue());

        String numerTabeliDlaTransakcji = wyciagnijNumerTabeli(pobraneDaneDlaTransakcji);
        String dzienTabeliDlaTransakcji = wyciagnijDzien(pobraneDaneDlaTransakcji);
        String kursDlaTransakcji = wyciagnijKurs(pobraneDaneDlaTransakcji);
        double przeliczonaKwotaDlaTransakcji = przeliczKwote(kursDlaTransakcji);

        //Twórz Array z parametrami
        String[] parameters;
        double kwotaVAT = 0;
        DecimalFormat format = new DecimalFormat("###,##0.00");

        if (rbVAT.isSelected()) {
            kwotaVAT = obliczVAT(przeliczonaKwotaDlaTransakcji);
            parameters = new String[6];
            parameters[5] = format.format(kwotaVAT) + " zł";
        } else {
            parameters = new String[5];
        }

        parameters[0] = format.format(Double.parseDouble(tbWartoscTransakcji.getText().replace(",", "."))) + " " + cbWaluty.getValue();
        parameters[1] = numerTabeliDlaTransakcji;
        parameters[2] = dzienTabeliDlaTransakcji;
        parameters[3] = kursDlaTransakcji.replace(".", ",");
        parameters[4] = format.format(przeliczonaKwotaDlaTransakcji) + " zł";

        if (rbDrukuj.isSelected()) {
            zapisywanieEtykiety(parameters);
            try {
                drukowanieEtykiety();
            } catch (PrinterException ex) {
            }
        }

        pokazPodsumowanie(parameters);

    }

//Metody pobierające
    private String pobierzDane(LocalDate dData, String sWpisanaWaluta) throws MalformedURLException, IOException {

        String pobraneDane = "";

        boolean retry = true;

        int loopCount = 0;

        while (retry) {

            //Połącz z API NBP i odczytaj dane (to też do odrębnej metody?)
            try {
                //Stwórz link do połączenia się z API NBP
                dData = dData.minusDays(1);
                String formatowanaData = dData.format(API_DATA_FORMAT);

                StringBuilder stringDoAPINBP = new StringBuilder(NBP_API_LINK);
                stringDoAPINBP.append(sWpisanaWaluta).append("/").append(formatowanaData).append("/?format=json");
                String linkDoAPINBP = stringDoAPINBP.toString();

                URL urlAPINBP = new URL(linkDoAPINBP);

                try (BufferedReader in = new BufferedReader(new InputStreamReader(urlAPINBP.openStream()))) {
                    pobraneDane = in.readLine();
                }

                return pobraneDane;

            } catch (FileNotFoundException ex) {
                if (loopCount > RETRY_COUNT) {
                    System.out.println("Przekroczono limit powtórzeń");
                }
                loopCount++;
            }
        }
        return pobraneDane;
    }

    private static String wyciagnijNumerTabeli(String sDane) {
        String numerTabeli = sDane.substring(sDane.indexOf("no") + 5, sDane.indexOf("[") + 22);
        return numerTabeli;
    }

    private static String wyciagnijDzien(String sDane) {
        String sDzienTabeli = sDane.substring(sDane.indexOf("effectiveDate") + 16, sDane.indexOf("[") + 51);
        return sDzienTabeli;
    }

    private String wyciagnijKurs(String sDane) {
        //Sprawdzenie, czy przypadkiem waluta nie jest groszowa i jeśli tak, to ew. dostosowanie kursu
        String sKurs = sDane.substring(sDane.indexOf("mid") + 5, sDane.indexOf("[") + 67);
        if (sKurs.charAt(sKurs.length() - 1) == ']') {
            sKurs = sKurs.substring(0, sKurs.length() - 1);
        }
        if (sKurs.charAt(sKurs.length() - 1) == '}') {
            sKurs = sKurs.substring(0, sKurs.length() - 1);
        }
        return sKurs;
    }

    private double przeliczKwote(String sKurs) {
        double przeliczonaKwota = Math.round((Double.parseDouble(tbWartoscTransakcji.getText().replace(",", ".")) * Double.parseDouble(sKurs)) * 100.00) / 100.00;
        return przeliczonaKwota;
    }

    private double obliczVAT(double przeliczonaKwotaDlaTransakcji) {
        double kwotaVAT = (Double.parseDouble(cbVAT.getValue().replace("%", "")) * przeliczonaKwotaDlaTransakcji) / 100;
        return kwotaVAT;
    }

    @FXML
    private void pokazPodsumowanie(String[] params) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Podsumowanie.fxml"));

        Pane root = (Pane) fxmlLoader.load();

        Stage stage = new Stage();
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/resources/mojStyl.css");

        stage.setScene(scene);
        stage.setTitle(PODSUMOWANIE_TITLE);
        stage.getIcons().add(new Image("/resources/brachs.png"));

        PodsumowanieController controller = fxmlLoader.<PodsumowanieController>getController();
        controller.generujPodsumowanie(params);

        stage.setResizable(false);
        stage.show();
    }

    @FXML
    private void zapisywanieEtykiety(String[] arguments) throws IOException {

        String[] stringArray = new String[10];

        stringArray[0] = " --------------------------";
        stringArray[1] = "   Kurs 1 " + cbWaluty.getValue() + " = " + arguments[3];
        stringArray[2] = "  wg tab.: " + arguments[1];
        stringArray[3] = "     z dn. " + arguments[2];
        stringArray[4] = " --------------------------";
        stringArray[5] = " " + arguments[0] + " * " + arguments[3];
        stringArray[6] = " = " + arguments[4];

        if (rbVAT.isSelected()) {
            stringArray[7] = " --------------------------";
            stringArray[8] = " VAT " + cbVAT.getValue() + " = " + arguments[5];
            stringArray[9] = " --------------------------";
        } else {
            stringArray[7] = " --------------------------";
            stringArray[8] = " ";
            stringArray[9] = " ";
        }

        File tempFile = new File(FILE_PATH);

        try ( //Plik jest nadpisywany
                BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String stringToBeSaved : stringArray) {
                writer.write(stringToBeSaved + System.lineSeparator());
            }
        }
    }

    private PrintService findPrintService(String printerName) {
        PrintService service = null;

        PrintService[] services = PrinterJob.lookupPrintServices();

        for (int i = 0; service == null && i < services.length; i++) {
            if (services[i].getName().equals(printerName)) {
                service = services[i];
            }
        }
        return service;
    }

    @FXML
    private void drukowanieEtykiety() throws PrinterException, IOException {

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Drukowanie(odczytywaniePliku()));
        job.setPrintService(findPrintService("Xprinter XP-350B"));

        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(OrientationRequested.PORTRAIT);
        aset.add(new MediaPrintableArea(0, 0, PAGE_HEIGHT, PAGE_WIDTH, MediaPrintableArea.MM));

        boolean doPrint = job.printDialog();
        if (doPrint) {
            job.print(aset);
        }
    }

    @FXML
    private String odczytywaniePliku() throws FileNotFoundException, IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(FILE_PATH)));
        StringBuilder stringBuilder = new StringBuilder();

        String textLine = bufferedReader.readLine();

        while (textLine != null) {
            stringBuilder.append(textLine);
            stringBuilder.append(System.lineSeparator());
            textLine = bufferedReader.readLine();
        }

        return stringBuilder.toString();
    }

    @FXML
    private void kopiowanieAdresu() {
        Tooltip tooltip = new Tooltip();
        tooltip.setText("Skopiowano adres e-mail");
        StringSelection selection = new StringSelection("jurek.rafal@outlook.com");
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        copyrightLabel.setTooltip(tooltip);
    }

    @FXML
    private void zamknijClick() {
        Stage stage = (Stage) bZamknij.getScene().getWindow();
        stage.close();
    }
}
