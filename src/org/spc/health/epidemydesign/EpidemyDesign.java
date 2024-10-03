/*
 Copyright - Pacific Community                   
 Droit de copie - Communauté du Pacifique 
 http://www.spc.int/                                                
*/
package org.spc.health.epidemydesign;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
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
    public void start(final Stage primaryStage) throws IOException {
        final var fxmlURL = getClass().getResource("MainUI.fxml"); // NOI18N.
        final var fxmlLoader = new FXMLLoader(fxmlURL, I18N.getResourceBundle());
        final var mainUI = fxmlLoader.<Node>load();
        final var mainUIController = fxmlLoader.<MainUIController>getController();
        mainUIController.setApplication(this);
        final var root = new StackPane(mainUI);
        final var scene = new Scene(root);
        Optional.ofNullable(getClass().getResource("EpidemyDesign.css"))  // NOI18N.
                .map(URL::toExternalForm)
                .ifPresent(scene.getStylesheets()::add);
        primaryStage.setTitle(String.format("%s %s", I18N.getString("app.title"), Settings.getVersion())); // NOI18N.
        stage = primaryStage;
        Optional.ofNullable(getClass().getResource("AppIcon.png")) // NOI18N.
                .map(URL::toExternalForm)
                .map(Image::new)
                .ifPresent(stage.getIcons()::add);
        stage.setScene(scene);
        stage.show();
        stage.xProperty().addListener(stagePropertiesInvalidationListener);
        stage.yProperty().addListener(stagePropertiesInvalidationListener);
        stage.widthProperty().addListener(stagePropertiesInvalidationListener);
        stage.heightProperty().addListener(stagePropertiesInvalidationListener);
    }

    private final InvalidationListener stagePropertiesInvalidationListener = _ -> Platform.runLater(() -> {
        Settings.getPrefs().putDouble("stage.x", stage.getX());
        Settings.getPrefs().putDouble("stage.y", stage.getY());
        Settings.getPrefs().putDouble("stage.width", stage.getWidth());
        Settings.getPrefs().putDouble("stage.height", stage.getHeight());
    });

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        launch(args);
    }

}
