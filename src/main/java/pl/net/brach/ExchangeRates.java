package pl.net.brach;

import java.io.*;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ExchangeRates extends Application {
    
    static final String BRACHSOFT_TITLE = "BRACHSoft - Kursy walut v.1.2";
    static final String ICON_PATH = "pl/net/brach/brachicon.png";
    static final String STYLE_PATH = "pl/net/brach/style.css";

    static final String PRINTER_NAME = "Xprinter XP-350B";
    static final List<String> AVAILABLE_CURRENCIES = Arrays.asList("EUR", "USD", "CHF", "GBP", "CZK", "RON", "HUF");
    static final List<String> AVAILABLE_VAT_RATES = Arrays.asList("23%", "8%", "5%");

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

    public static void main(String[] args) {
        launch(args);
    }
}
