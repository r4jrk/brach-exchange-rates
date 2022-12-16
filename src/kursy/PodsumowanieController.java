package kursy;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

public class PodsumowanieController implements Initializable {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);

    @FXML
    private Button bZamknij;
    public Label transakcjaNumerTabeli;
    public Label transakcjaDataKursu;
    public Label transakcjaKwota;
    public Label transakcjaKurs;
    public Label transakcjaPrzeliczonaKwota;
    public Label transakcjaKwotaVATLabel;
    public Label transakcjaKwotaVAT;
    public Line bottomGridLine;
    public BorderPane podsumowanieWindow;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    @FXML
    public void generujPodsumowanie(String[] params) {
        transakcjaNumerTabeli.setText(params[1]);
        transakcjaDataKursu.setText(LocalDate.parse(params[2]).format(DATE_FORMAT));
        transakcjaKwota.setText(params[0]);
        transakcjaKurs.setText(params[3]);
        transakcjaPrzeliczonaKwota.setText(params[4]);

        if (params.length == 6) {
            transakcjaKwotaVAT.setText(params[5]);
            transakcjaKwotaVATLabel.setVisible(true);
            transakcjaKwotaVAT.setVisible(true);
        } else {
            bottomGridLine.setLayoutY(152.0);
            bZamknij.setLayoutY(177.0);
            podsumowanieWindow.setPrefHeight(310.0);
        }
    }

    @FXML
    private void zamknijClick() {
        Stage stage = (Stage) bZamknij.getScene().getWindow();
        stage.close();
    }
}
