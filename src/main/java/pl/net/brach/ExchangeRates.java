package pl.net.brach;

import java.io.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class ExchangeRates extends Application {
    
    static final String BRACHSOFT_TITLE = "BRACHSoft - Kursy walut v.1.2";
    static final String ICON_PATH = "pl/net/brach/brachicon.png";
    static final String STYLE_PATH = "pl/net/brach/style.css";

    static final String PRIMARY_PRINTER_NAME = "Xprinter XP-350B";
    static final String SECONDARY_PRINTER_NAME = "Xprinter XP-420B";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("MainWindow.fxml"));
        Parent root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLE_PATH);

        stage.setTitle(BRACHSOFT_TITLE);
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    protected static void displaySummary(String[] args) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ExchangeRates.class.getResource("Summary.fxml"));
        Pane root = fxmlLoader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(STYLE_PATH);

        Stage stage = new Stage();

        stage.setTitle(BRACHSOFT_TITLE + " - Podsumowanie");
        stage.getIcons().add(new Image(ICON_PATH));
        stage.setScene(scene);
        stage.setResizable(false);

        SummaryController summaryController = fxmlLoader.getController();
        summaryController.generateSummary(args);

        stage.show();
    }
}
