/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign.control.generatepane;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.stage.DirectoryChooser;
import org.spc.health.epidemydesign.ControllerBase;
import org.spc.health.epidemydesign.Settings;

/**
 * Controller for the generate pane.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class GeneratePaneController extends ControllerBase implements Initializable {

    @FXML
    private ComboBox<String> targetComboBox;
    @FXML
    private ProgressBar generateProgressBar;

    /**
     * Creates a new instance.
     */
    public GeneratePaneController() {
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        targetComboBox.getEditor().textProperty().addListener(targetPathInvalidationListener);
        generateProgressBar.progressProperty().bind(progressProperty());
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the text in the target folder combo editor is changed.
     */
    private final InvalidationListener targetPathInvalidationListener = (Observable observable) -> {
        Platform.runLater(() -> {
            final String path = targetComboBox.getEditor().getText();
            final File file = new File(path);
            if (file.exists() && file.isDirectory()) {
                Settings.getPrefs().put("last.output.folder", path); // NOI18N.
            }
        });
    };

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the browse button of the target folder is clicked.
     */
    @FXML
    private void handleTargetBrowseButton(final ActionEvent actionEvent) {
        final String userHome = System.getProperty("user.home"); // NOI18N.
        final String path = Settings.getPrefs().get("last.output.folder", userHome); // NOI18N.
        File folder = new File(path);
        folder = (!folder.exists() || !folder.isDirectory()) ? new File(userHome) : folder; // NOI18N.
        final DirectoryChooser dialog = new DirectoryChooser();
        dialog.setInitialDirectory(folder);
        folder = dialog.showDialog(targetComboBox.getScene().getWindow());
        if (folder != null) {
            final String newPath = folder.getAbsolutePath();
            Settings.getPrefs().put("last.output.folder", newPath); // NOI18N.
            targetComboBox.setValue(folder.getAbsolutePath());
            if (!targetComboBox.getItems().contains(newPath)) {
                targetComboBox.getItems().add(0, newPath);
            }
        }
    }

    /**
     * Called whenever the generate button is clicked.
     */
    @FXML
    private void handleGenerateButton(final ActionEvent actionEvent) {
        Optional<EventHandler<ActionEvent>> onGenerate = Optional.ofNullable(getOnGenerate());
        onGenerate.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }
    ////////////////////////////////////////////////////////////////////////////
    private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress"); // NOI18N.

    public final double getProgress() {
        return progress.get();
    }

    public final void setProgress(final double value) {
        progress.set(value);
    }

    public final DoubleProperty progressProperty() {
        return progress;
    }

    /**
     * What to do when the generate button is clicked.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onGenerate = new SimpleObjectProperty<>(this, "onGenerate"); // NOI18N.

    public final EventHandler<ActionEvent> getOnGenerate() {
        return onGenerate.get();
    }

    public final void setOnGenerate(final EventHandler<ActionEvent> value) {
        onGenerate.set(value);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onGenerateProperty() {
        return onGenerate;
    }

}
