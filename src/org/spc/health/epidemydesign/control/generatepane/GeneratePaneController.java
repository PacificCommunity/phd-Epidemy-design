/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign.control.generatepane;

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

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the generate pane.
 *
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class GeneratePaneController extends ControllerBase implements Initializable {

    ////////////////////////////////////////////////////////////////////////////
    private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress"); // NOI18N.
    /**
     * What to do when the generate button is clicked.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onGenerate = new SimpleObjectProperty<>(this, "onGenerate"); // NOI18N.
    @FXML
    private ComboBox<String> targetComboBox;
    /**
     * Called whenever the text in the target folder combo editor is changed.
     */
    private final InvalidationListener targetPathInvalidationListener = (Observable _) -> Platform.runLater(() -> {
        final var path = targetComboBox.getEditor().getText();
        final var file = new File(path);
        if (file.exists() && file.isDirectory()) {
            Settings.getPrefs().put("last.output.folder", path); // NOI18N.
        }
    });

    ////////////////////////////////////////////////////////////////////////////
    @FXML
    private ProgressBar generateProgressBar;

    ////////////////////////////////////////////////////////////////////////////

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

    /**
     * Called whenever the browse button of the target folder is clicked.
     */
    @FXML
    private void handleTargetBrowseButton(final ActionEvent actionEvent) {
        final var userHome = System.getProperty("user.home"); // NOI18N.
        final var path = Settings.getPrefs().get("last.output.folder", userHome); // NOI18N.
        var folder = new File(path);
        folder = (!folder.exists() || !folder.isDirectory()) ? new File(userHome) : folder; // NOI18N.
        final var dialog = new DirectoryChooser();
        dialog.setInitialDirectory(folder);
        Optional.ofNullable(dialog.showDialog(targetComboBox.getScene().getWindow()))
                .ifPresent(userFolder -> {
                    final String newPath = userFolder.getAbsolutePath();
                    Settings.getPrefs().put("last.output.folder", newPath); // NOI18N.
                    targetComboBox.setValue(userFolder.getAbsolutePath());
                    if (!targetComboBox.getItems().contains(newPath)) {
                        targetComboBox.getItems().addFirst(newPath);
                    }
                });
    }

    /**
     * Called whenever the generate button is clicked.
     */
    @FXML
    private void handleGenerateButton(final ActionEvent actionEvent) {
        Optional.ofNullable(getOnGenerate())
                .ifPresent(eventHandler -> eventHandler.handle(new ActionEvent(this, null)));
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(final double value) {
        progress.set(value);
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public EventHandler<ActionEvent> getOnGenerate() {
        return onGenerate.get();
    }

    public void setOnGenerate(final EventHandler<ActionEvent> value) {
        onGenerate.set(value);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onGenerateProperty() {
        return onGenerate;
    }

}
