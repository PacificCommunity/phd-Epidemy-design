/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign.control.infectioneditor;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.spc.health.epidemydesign.ControllerBase;
import org.spc.health.epidemydesign.Infection;
import org.spc.health.epidemydesign.State;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Controller for the infection editor.
 *
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class InfectionEditorController extends ControllerBase implements Initializable {

    /**
     * Called whenever the infections list changes.
     */
    private final ListChangeListener<Infection> infectionsListChangeListener = _ -> {
    };
    /**
     * List of infections.
     */
    private final ListProperty<Infection> infections = new SimpleListProperty<>(this, "infections", FXCollections.observableList(new LinkedList<>())); // NOI18N.
    /**
     * List of states.
     */
    private final ListProperty<State> states = new SimpleListProperty<>(this, "states", FXCollections.observableList(new LinkedList<>())); // NOI18N.
    /**
     * List of recent files.
     */
    private final ListProperty<File> recentFiles = new SimpleListProperty<>(this, "recentFiles", FXCollections.observableList(new LinkedList<>())); // NOI18N.
    /**
     * What to do when saving infection definitions.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onSave = new SimpleObjectProperty<>(this, "onSave"); // NOI18N.
    /**
     * What to do when loading infection definitions.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onLoad = new SimpleObjectProperty<>(this, "onLoad"); // NOI18N.
    /**
     * What to do when resetting infection definitions.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onDefault = new SimpleObjectProperty<>(this, "onDefault"); // NOI18N.
    /**
     * What to do when loading a specific infection definitions file.
     */
    private final ObjectProperty<Consumer<File>> onSelectFile = new SimpleObjectProperty<>(this, "onSelectFile"); // NOI18N.
    @FXML
    private TableView<Infection> infectionTable;

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the states list changes.
     */
    private final ListChangeListener<State> statesListChangeListener = change -> {
        while (change.next()) {
            // Remove states.
            change.getRemoved().forEach(state -> {
                final var toRemove = infectionTable.getColumns()
                        .stream()
                        .filter(tableColumn -> state.equals(tableColumn.getProperties().get("state"))) // NOI18N.
                        .collect(Collectors.toList());
                toRemove.forEach(tableColumn -> {
                    infectionTable.getColumns().remove(tableColumn);
                    tableColumn.getProperties().remove("state"); // NOI18N.
                });
                toRemove.clear();
            });
            // Add states.
            change.getAddedSubList().forEach(state -> {
                final var tableColumn = createTableColumnForState(state);
                infectionTable.getColumns().add(tableColumn);
            });
        }
    };
    @FXML
    private TableColumn<Infection, String> infectionNameColumn;
    @FXML
    private TableColumn<Infection, String> infectionFileColumn;

    ////////////////////////////////////////////////////////////////////////////
    @FXML
    private TextField infectionsField;
    @FXML
    private Button addInfectionsButton;
    @FXML
    private Button deleteInfectionsButton;
    @FXML
    private SplitMenuButton loadInfectionsButton;
    /**
     * Called whenever the recent file list changes.
     */
    private final ListChangeListener<File> recentFilesListChangeListener = change -> {
        while (change.next()) {
            // Remove files.
            change.getRemoved().forEach(file -> {
                final var toRemove = loadInfectionsButton.getItems()
                        .stream()
                        .filter(menuItem -> file.equals(menuItem.getProperties().get("file"))) // NOI18N.
                        .collect(Collectors.toList());
                toRemove.forEach(menuItem -> {
                    loadInfectionsButton.getItems().remove(menuItem);
                    menuItem.getProperties().remove("file"); // NOI18N.
                    menuItem.setOnAction(null);
                });
                toRemove.clear();
            });
            // Add files.
            change.getAddedSubList().forEach(file -> {
                final var fileMenuItem = new MenuItem(file.getAbsolutePath());
                fileMenuItem.getProperties().put("file", file); // NOI18N.
                fileMenuItem.setOnAction(_ -> Optional.ofNullable(getOnSelectFile())
                        .ifPresent(consumer -> consumer.accept(file)));
                loadInfectionsButton.getItems().add(fileMenuItem);
            });
        }
    };

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new instance.
     */
    public InfectionEditorController() {
        infections.addListener(infectionsListChangeListener);
        states.addListener(statesListChangeListener);
        recentFiles.addListener(recentFilesListChangeListener);
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        //
        infectionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        infectionTable.setEditable(true);
        infectionTable.setItems(infections);
        //
        infectionNameColumn.setCellValueFactory(new PropertyValueFactory("name")); // NOI18N.
        infectionNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        infectionNameColumn.setEditable(true);
        //
        infectionFileColumn.setCellValueFactory(new PropertyValueFactory("fileName")); // NOI18N.
        infectionFileColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        infectionFileColumn.setEditable(true);
        //
        addInfectionsButton.disableProperty().bind(Bindings.isEmpty(infectionsField.textProperty()));
        deleteInfectionsButton.disableProperty().bind(Bindings.isEmpty(infectionTable.getSelectionModel().getSelectedCells()));
    }

    /**
     * Called whenever the add button is clicked.
     */
    @FXML
    private void handleAddInfectionsButton(final ActionEvent actionEvent) {
        final var name = infectionsField.getText().trim();
        if (name.isBlank()) {
            return;
        }
        final var newInfection = new Infection(name, null);
        final var infectionList = new LinkedList<>(infections);
        infectionList.add(newInfection);
        Collections.sort(infectionList);
        infections.setAll(infectionList);
        infectionList.clear();
        infectionTable.getSelectionModel().select(newInfection);
    }

    /**
     * Called whenever the delete button is clicked.
     */
    @FXML
    private void handleDeleteInfectionsButton(final ActionEvent actionEvent) {
        final var toRemove = new LinkedList<>(infectionTable.getSelectionModel().getSelectedItems());
        infections.removeAll(toRemove);
        toRemove.clear();
    }

    /**
     * Called whenever the save button is clicked.
     */
    @FXML
    private void handleSaveInfectionsButton(final ActionEvent actionEvent) {
        Optional.ofNullable(getOnSave())
                .ifPresent(eventHandler -> eventHandler.handle(new ActionEvent(this, null)));
    }

    /**
     * Called whenever the load button is clicked.
     */
    @FXML
    private void handleLoadInfectionsButton(final ActionEvent actionEvent) {
        Optional.ofNullable(getOnLoad())
                .ifPresent(eventHandler -> eventHandler.handle(new ActionEvent(this, null)));
    }

    /**
     * Called whenever the reset button is clicked.
     */
    @FXML
    private void handleInfectionsDefaultButton(final ActionEvent actionEvent) {
        Optional.ofNullable(getOnDefault())
                .ifPresent(eventHandler -> eventHandler.handle(new ActionEvent(this, null)));
    }

    /**
     * Create a new columns in the table for a given state.
     *
     * @param state The state.
     */
    private TableColumn<Infection, Boolean> createTableColumnForState(final State state) {
        final var result = new TableColumn<Infection, Boolean>(state.getName());
        result.getProperties().put("state", state); // NOI18N.
        result.setEditable(true);
        result.setCellValueFactory(features -> {
            final var infection = features.getValue();
            final boolean activated = infection.getStates().contains(state);
            final var property = new SimpleBooleanProperty(activated);
            property.addListener((_, _, newValue) -> {
                if (newValue) {
                    infection.getStates().add(state);
                } else {
                    infection.getStates().remove(state);
                }
            });
            return property;
        });
        result.setCellFactory(CheckBoxTableCell.forTableColumn(result));
        return result;
    }

    public ObservableList<Infection> getInfections() {
        return infections.get();
    }

    public void setInfections(final ObservableList<Infection> value) {
        infections.set(value);
    }

    public ListProperty<Infection> infectionsProperty() {
        return infections;
    }

    public ObservableList<State> getStates() {
        return states.get();
    }

    public void setStates(final ObservableList<State> value) {
        states.set(value);
    }

    public ListProperty<State> statesProperty() {
        return states;
    }

    public ObservableList<File> getRecentFiles() {
        return recentFiles.get();
    }

    public void setRecentFiles(final ObservableList<File> value) {
        recentFiles.set(value);
    }

    public ListProperty<File> recentFilesProperty() {
        return recentFiles;
    }

    public EventHandler<ActionEvent> getOnSave() {
        return onSave.get();
    }

    public void setOnSave(final EventHandler<ActionEvent> value) {
        onSave.set(value);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onSaveProperty() {
        return onSave;
    }

    public EventHandler<ActionEvent> getOnLoad() {
        return onLoad.get();
    }

    public void setOnLoad(final EventHandler<ActionEvent> value) {
        onLoad.set(value);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onLoadProperty() {
        return onLoad;
    }

    public EventHandler<ActionEvent> getOnDefault() {
        return onDefault.get();
    }

    public void setOnDefault(final EventHandler<ActionEvent> value) {
        onDefault.set(value);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onDefaultProperty() {
        return onDefault;
    }

    public Consumer<File> getOnSelectFile() {
        return onSelectFile.get();
    }

    public void setOnSelectFile(final Consumer<File> value) {
        onSelectFile.set(value);
    }

    public ObjectProperty<Consumer<File>> onSelectFileProperty() {
        return onSelectFile;
    }
}
