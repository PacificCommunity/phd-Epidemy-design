/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign.control.infectioneditor;

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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import org.spc.health.epidemydesign.ControllerBase;
import org.spc.health.epidemydesign.Infection;
import org.spc.health.epidemydesign.State;

/**
 * Controller for the infection editor.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class InfectionEditorController extends ControllerBase implements Initializable {

    @FXML
    private TableView<Infection> infectionTable;
    @FXML
    private TableColumn<Infection, String> infectionNameColumn;
    @FXML
    private TableColumn<Infection, String> infectionFileColumn;
    @FXML
    private TextField infectionsField;
    @FXML
    private Button addInfectionsButton;
    @FXML
    private Button deleteInfectionsButton;
    @FXML
    private SplitMenuButton loadInfectionsButton;

    /**
     * Creates a new instance.
     */
    public InfectionEditorController() {
        infections.addListener(infectionsListChangeListener);
        states.addListener(statesListChangeListener);
        recentFiles.addListener(recentFilesListChangeListener);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the infections list changes.
     */
    private final ListChangeListener<Infection> infectionsListChangeListener = (final Change<? extends Infection> change) -> {
    };

    /**
     * Called whenever the states list changes.
     */
    private final ListChangeListener<State> statesListChangeListener = (final Change<? extends State> change) -> {
        while (change.next()) {
            // Remove states.
            change.getRemoved().forEach((final State state) -> {
                final List<TableColumn> toRemove = infectionTable.getColumns().stream()
                        .filter((final TableColumn tableColumn) -> state.equals(tableColumn.getProperties().get("state"))) // NOI18N.
                        .collect(Collectors.toList());
                toRemove.forEach((final TableColumn tableColumn) -> {
                    infectionTable.getColumns().remove(tableColumn);
                    tableColumn.getProperties().remove("state"); // NOI18N.
                });
                toRemove.clear();
            });
            // Add states.
            change.getAddedSubList().forEach((final State state) -> {
                final TableColumn<Infection, Boolean> tableColumn = createTableColumnForState(state);
                infectionTable.getColumns().add(tableColumn);
            });
        }
    };

    /**
     * Called whenever the recent file list changes.
     */
    private final ListChangeListener<File> recentFilesListChangeListener = (final Change<? extends File> change) -> {
        while (change.next()) {
            // Remove files.
            change.getRemoved().forEach((final File file) -> {
                final List<MenuItem> toRemove = loadInfectionsButton.getItems().stream()
                        .filter((final MenuItem menuItem) -> file.equals(menuItem.getProperties().get("file"))) // NOI18N.
                        .collect(Collectors.toList());
                toRemove.forEach((final MenuItem menuItem) -> {
                    loadInfectionsButton.getItems().remove(menuItem);
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
                loadInfectionsButton.getItems().add(fileMenuItem);
            });
        }
    };

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the add button is clicked.
     */
    @FXML
    private void handleAddInfectionsButton(final ActionEvent actionEvent) {
        final String name = infectionsField.getText().trim();
        if (name.isEmpty()) {
            return;
        }
        final Infection newInfection = new Infection(name, null);
        final List<Infection> infectionList = new LinkedList<>(infections);
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
        final List<Infection> toRemove = new LinkedList<>(infectionTable.getSelectionModel().getSelectedItems());
        infections.removeAll(toRemove);
        toRemove.clear();
    }

    /**
     * Called whenever the save button is clicked.
     */
    @FXML
    private void handleSaveInfectionsButton(final ActionEvent actionEvent) {
        final Optional<EventHandler<ActionEvent>> onSave = Optional.ofNullable(getOnSave());
        onSave.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }

    /**
     * Called whenever the load button is clicked.
     */
    @FXML
    private void handleLoadInfectionsButton(final ActionEvent actionEvent) {
        final Optional<EventHandler<ActionEvent>> onLoad = Optional.ofNullable(getOnLoad());
        onLoad.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }

    /**
     * Called whenever the reset button is clicked.
     */
    @FXML
    private void handleInfectionsDefaultButton(final ActionEvent actionEvent) {
        final Optional<EventHandler<ActionEvent>> onDefault = Optional.ofNullable(getOnDefault());
        onDefault.ifPresent((final EventHandler<ActionEvent> eventHandler) -> eventHandler.handle(new ActionEvent(this, null)));
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Create a new columns in the table for a given state.
     * @param state The state.
     */
    private TableColumn<Infection, Boolean> createTableColumnForState(final State state) {
        final TableColumn<Infection, Boolean> result = new TableColumn<>(state.getName());
        result.getProperties().put("state", state); // NOI18N.
        result.setEditable(true);
        result.setCellValueFactory((TableColumn.CellDataFeatures<Infection, Boolean> p) -> {
            final Infection infection = p.getValue();
            final boolean activated = infection.getStates().contains(state);
            final BooleanProperty property = new SimpleBooleanProperty(activated);
            property.addListener((ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) -> {
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

    ////////////////////////////////////////////////////////////////////////////
    /**
     * List of infections.
     */
    private final ListProperty<Infection> infections = new SimpleListProperty<>(this, "infections", FXCollections.observableList(new LinkedList())); // NOI18N.

    public final ObservableList<Infection> getInfections() {
        return infections.get();
    }

    public final void setInfections(final ObservableList<Infection> value) {
        infections.set(value);
    }

    public final ListProperty<Infection> infectionsProperty() {
        return infections;
    }

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
