package pl.net.brach;

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

public class SummaryController implements Initializable {

    private static final double SUMMARY_WINDOW_PREFERRED_HEIGHT = 310;
    private static final double BOTTOM_GRID_LINE_POSITION_VERTICAL = 152.0;
    private static final double BUTTON_CLOSE_POSITION_VERTICAL = 177.0;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH);

    public Button bClose;
    public Label transactionTableNumber;
    public Label transactionRateDate;
    public Label transactionAmount;
    public Label transactionRate;
    public Label transactionCalculatedAmount;
    public Label transactionAmountVATLabel;
    public Label transactionAmountVAT;
    public Line bottomGridLine;
    public BorderPane summaryWindow;

    @Override
    public void initialize(URL url, ResourceBundle rb) { }

    @FXML
    public void generateSummary(String[] params) {
        transactionTableNumber.setText(params[1]);
        transactionRateDate.setText(LocalDate.parse(params[2]).format(DATE_FORMAT));
        transactionAmount.setText(params[0]);
        transactionRate.setText(params[3]);
        transactionCalculatedAmount.setText(params[4]);

        if (params.length == 6) {
            transactionAmountVAT.setText(params[5]);
            transactionAmountVATLabel.setVisible(true);
            transactionAmountVAT.setVisible(true);
        } else {
            bottomGridLine.setLayoutY(BOTTOM_GRID_LINE_POSITION_VERTICAL);
            bClose.setLayoutY(BUTTON_CLOSE_POSITION_VERTICAL);
            summaryWindow.setPrefHeight(SUMMARY_WINDOW_PREFERRED_HEIGHT);
        }
    }

    @FXML
    private void closeClicked() {
        Stage stage = (Stage) bClose.getScene().getWindow();
        stage.close();
    }
}
