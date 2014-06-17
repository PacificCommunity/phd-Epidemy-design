/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign.control.stateeditor;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import org.spc.health.epidemydesign.ControllerBase;
import org.spc.health.epidemydesign.State;

/**
 * Controller for the state editor.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class StateEditorController extends ControllerBase implements Initializable {

    @FXML
    private ListView<State> stateListView;
    @FXML
    private TextField statesField;
    @FXML
    private Button addStatesButton;
    @FXML
    private SplitMenuButton loadStatesButton;
    @FXML
    private Button deleteStatesButton;

    /**
     * Creates a new instance.
     */
    public StateEditorController() {
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        stateListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        stateListView.setItems(states);
        //
        addStatesButton.disableProperty().bind(Bindings.isEmpty(statesField.textProperty()));
        deleteStatesButton.disableProperty().bind(Bindings.isEmpty(stateListView.getSelectionModel().getSelectedItems()));
    }
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Called whenever the recent file list changes.
     */
    private final ListChangeListener<File> recentFilesListChangeListener = (final ListChangeListener.Change<? extends File> change) -> {
        while (change.next()) {
            // Remove files.
            change.getRemoved().forEach((final File file) -> {
                final List<MenuItem> toRemove = loadStatesButton.getItems().stream()
                        .filter((final MenuItem menuItem) -> file.equals(menuItem.getProperties().get("file"))) // NOI18N.
                        .collect(Collectors.toList());
                toRemove.forEach((final MenuItem menuItem) -> {
                    loadStatesButton.getItems().remove(menuItem);
                    menuItem.getProperties().remove("file"); // NOI18N.
                    menuItem.setOnAction(null);
                });
                toRemove.clear();
            });
            // Add files.
            change.getAddedSubList().forEach((final File file) -> {
                final MenuItem fileMenuItem = new MenuItem(file.getAbsolutePath());
                fileMenuItem.getProperties().put("file", file); // NOI18N.
                fileMenuItem.setOnAction((final ActionEvent actionEvent) -> {
                    final Optional<Consumer<File>> onSelectFile = Optional.ofNullable(getOnSelectFile());
                    onSelectFile.ifPresent((final Consumer<File> consumer) -> consumer.accept(file));
                });
                loadStatesButton.getItems().add(fileMenuItem);
            });
        }
    };

    ////////////////////////////////////////////////////////////////////////////
    @FXML
    private void handleStatesSaveButton(final ActionEvent actionEvent) {
        final Optional<EventHandler<ActionEvent>> onSave = Optional.ofNullable(getOnSave());
        onSave.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }

    @FXML
    private void handleStatesLoadButton(final ActionEvent actionEvent) {
        final Optional<EventHandler<ActionEvent>> onLoad = Optional.ofNullable(getOnLoad());
        onLoad.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }

    @FXML
    private void handleStatesDefaultButton(final ActionEvent actionEvent) {
        final Optional<EventHandler<ActionEvent>> onDefault = Optional.ofNullable(getOnDefault());
        onDefault.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }

    @FXML
    private void handleStatesAddButton(final ActionEvent actionEvent) {
        final String name = statesField.getText().trim();
        if (!name.isEmpty()) {
            final State newState = new State(name, null);
            final List<State> statesList = new LinkedList<>(states);
            statesList.add(newState);
            Collections.sort(statesList);
            states.setAll(statesList);
            stateListView.getSelectionModel().select(newState);
            statesList.clear();
        }
    }

    @FXML
    private void handleStatesDeleteButton(final ActionEvent actionEvent) {
        final List<State> toRemove = new LinkedList<>(stateListView.getSelectionModel().getSelectedItems());
        states.removeAll(toRemove);
        toRemove.clear();
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * List of states.
     */
    private final ListProperty<State> states = new SimpleListProperty<>(this, "states", FXCollections.observableList(new LinkedList())); // NOI18N.

    public final ObservableList<State> getStates() {
        return states.get();
    }

    public final void setStates(final ObservableList<State> value) {
        states.set(value);
    }

    public final ListProperty<State> statesProperty() {
        return states;
    }

    /**
     * List of recent files.
     */
    private final ListProperty<File> recentFiles = new SimpleListProperty<>(this, "recentFiles", FXCollections.observableList(new LinkedList())); // NOI18N.

    public final ObservableList<File> getRecentFiles() {
        return recentFiles.get();
    }

    public final void setRecentFiles(final ObservableList<File> value) {
        recentFiles.set(value);
    }

    public final ListProperty<File> recentFilesProperty() {
        return recentFiles;
    }

    /**
     * What to do when saving infection definitions.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onSave = new SimpleObjectProperty<>(this, "onSave"); // NOI18N.

    public final EventHandler<ActionEvent> getOnSave() {
        return onSave.get();
    }

    public final void setOnSave(final EventHandler<ActionEvent> value) {
        onSave.set(value);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onSaveProperty() {
        return onSave;
    }

    /**
     * What to do when loading infection definitions.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onLoad = new SimpleObjectProperty<>(this, "onLoad"); // NOI18N.

    public final EventHandler<ActionEvent> getOnLoad() {
        return onLoad.get();
    }

    public final void setOnLoad(final EventHandler<ActionEvent> value) {
        onLoad.set(value);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onLoadProperty() {
        return onLoad;
    }

    /**
     * What to do when resetting infection definitions.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onDefault = new SimpleObjectProperty<>(this, "onDefault"); // NOI18N.

    public final EventHandler<ActionEvent> getOnDefault() {
        return onDefault.get();
    }

    public final void setOnDefault(final EventHandler<ActionEvent> value) {
        onDefault.set(value);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onDefaultProperty() {
        return onDefault;
    }

    /**
     * What to do when loading a specific infection definitions file.
     */
    private final ObjectProperty<Consumer<File>> onSelectFile = new SimpleObjectProperty<>(this, "onSelectFile"); // NOI18N.

    public final Consumer<File> getOnSelectFile() {
        return onSelectFile.get();
    }

    public final void setOnSelectFile(final Consumer<File> value) {
        onSelectFile.set(value);
    }

    public final ObjectProperty<Consumer<File>> onSelectFileProperty() {
        return onSelectFile;
    }
}
