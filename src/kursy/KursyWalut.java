package kursy;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class KursyWalut extends Application {
    
    private static final String APPLICATION_TITLE = "BRACHSoft - Kursy walut v.1.11";

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("MainWindow.fxml"));

        Scene scene = new Scene(root);

        scene.getStylesheets().add("/resources/mojStyl.css");

        stage.setTitle(APPLICATION_TITLE);
        stage.getIcons().add(new Image("/resources/brachs.png"));
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
