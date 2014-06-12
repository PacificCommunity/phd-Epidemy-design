/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The app class.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class EpidemyDesign extends Application {

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws IOException {
        final URL fxmlURL = getClass().getResource("MainUI.fxml"); // NOI18N.
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlURL, I18N.getResourceBundle());
        final Node mainUI = fxmlLoader.load();
        final MainUIController mainUIController = fxmlLoader.getController();
        mainUIController.setApplication(this);
        final StackPane root = new StackPane(mainUI);
        final Scene scene = new Scene(root, 1200, 800);
        final URL cssURL = getClass().getResource("EpidemyDesign.css"); // NOI18N.
        scene.getStylesheets().add(cssURL.toExternalForm());
        primaryStage.setTitle("Epidemy Design"); // NOI18N.
        final URL iconURL = getClass().getResource("AppIcon.png"); // NOI18N.
        final Image icon = new Image(iconURL.toExternalForm());
        stage = primaryStage;
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
        stage.xProperty().addListener(stagePropertiesInvalidationListener);
        stage.yProperty().addListener(stagePropertiesInvalidationListener);
        stage.widthProperty().addListener(stagePropertiesInvalidationListener);
        stage.heightProperty().addListener(stagePropertiesInvalidationListener);
    }

    private final InvalidationListener stagePropertiesInvalidationListener = (Observable observable) -> {
        Platform.runLater(() -> {
            Settings.getPrefs().putDouble("stage.x", stage.getX());
            Settings.getPrefs().putDouble("stage.y", stage.getY());
            Settings.getPrefs().putDouble("stage.width", stage.getWidth());
            Settings.getPrefs().putDouble("stage.height", stage.getHeight());
        });
    };

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
