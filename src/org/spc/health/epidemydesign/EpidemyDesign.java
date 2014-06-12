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

    @Override
    public void start(Stage primaryStage) throws IOException {
        final ResourceBundle bundle = ResourceBundle.getBundle("org.spc.health.epidemydesign.strings"); // NOI18N.
        final URL fxmlURL = getClass().getResource("MainUI.fxml"); // NOI18N.
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlURL, bundle);
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
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
