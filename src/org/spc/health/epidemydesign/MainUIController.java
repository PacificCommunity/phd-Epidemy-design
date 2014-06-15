/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.WorkerStateEvent;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.spc.health.epidemydesign.control.codeeditor.CodeEditor;
import org.spc.health.epidemydesign.task.GenerationTask;

/**
 * The main UI controller.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class MainUIController extends ControllerBase implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MainUIController.class.getName());

    @FXML
    private TableView<Infection> infectionTable;
    @FXML
    private TableColumn<Infection, String> infectionNameColumn;
    @FXML
    private TableColumn<Infection, String> infectionFileColumn;
    @FXML
    private VBox cssContent;
    @FXML
    private VBox fxmlContent;
    @FXML
    private VBox previewPane;
    @FXML
    private ComboBox<Infection> previewCombo;
    @FXML
    private ComboBox<String> targetComboBox;
    @FXML
    private ProgressBar generateProgressBar;

    private final File homeFolder;
    private final File templateFolder;
    private final File fxmlFile;
    private final File cssFile;
    private final File infectionsFile;
    private final File statesFile;
    private final ObservableList<State> states = FXCollections.observableList(new LinkedList<>());
    private final ObservableList<Infection> infections = FXCollections.observableList(new LinkedList<>());

    private CodeEditor cssEditor;
    private CodeEditor fxmlEditor;

    public MainUIController() throws IOException {
        homeFolder = new File(System.getProperty("user.home"), ".EpidemyDesign"); // NOI18N.
        if (!homeFolder.exists()) {
            homeFolder.mkdirs();
        }
        templateFolder = new File(homeFolder, "template"); // NOI18N.
        if (!templateFolder.exists()) {
            templateFolder.mkdirs();
        }
        fxmlFile = new File(templateFolder, "template.fxml"); // NOI18N.
        if (!fxmlFile.exists()) {
            exportFXMLFromSource();
        }
        cssFile = new File(templateFolder, "template.css"); // NOI18N.
        if (!cssFile.exists()) {
            exportCSSFromSource();
        }
        infectionsFile = new File(templateFolder, "infections.properties"); // NOI18N.
        if (!infectionsFile.exists()) {
            exportInfectionsFromSource();
        }
        statesFile = new File(templateFolder, "states.properties"); // NOI18N.
        if (!statesFile.exists()) {
            exportStatesFromSource();
        }
    }
    
    /**
    * This binding is used to control whenever the code editor have been initialized.
    */
    private BooleanBinding codeEditorInitialized;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            reloadStatesFromTemplate();
            reloadInfectionsFromTemplate();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        infections.addListener((final ListChangeListener.Change<? extends Infection> change) -> {
            final List<Infection> comboList = new LinkedList<>();
            comboList.add(null);
            comboList.addAll(infections);
            previewCombo.getItems().setAll(comboList);
            comboList.clear();
        });
        //
        infectionTable.setEditable(true);
        infectionTable.setItems(infections);
        infectionNameColumn.setCellValueFactory((final TableColumn.CellDataFeatures<Infection, String> cellDataFeature) -> {
            final Infection infection = cellDataFeature.getValue();
            final String name = infection.getName();
            return new SimpleStringProperty(name);
        });
        infectionFileColumn.setCellValueFactory((final TableColumn.CellDataFeatures<Infection, String> cellDataFeature) -> {
            final Infection infection = cellDataFeature.getValue();
            final String fileName = infection.getFileName();
            return new SimpleStringProperty(fileName);
        });
        populateStatesColumns();
        //
        previewCombo.valueProperty().addListener(previewSelectionInvalidationListener);
        previewCombo.setButtonCell(new InfectionListCell());
        previewCombo.setCellFactory((final ListView<Infection> listView) -> {
            return new InfectionListCell();
        });
        previewCombo.setValue(null);
        //
        targetComboBox.getEditor().textProperty().addListener(targetPathInvalidationListener);
        // CSS editor.
        cssEditor = new CodeEditor();
        VBox.setVgrow(cssEditor, Priority.ALWAYS);
        cssContent.getChildren().add(cssEditor);
        // FXML editor.
        fxmlEditor = new CodeEditor();
        VBox.setVgrow(fxmlEditor, Priority.ALWAYS);
        fxmlContent.getChildren().add(fxmlEditor);
        //
        codeEditorInitialized = new BooleanBinding() {
            {
                bind(cssEditor.initializedProperty(), fxmlEditor.initializedProperty());
            }

            @Override
            public void dispose() {
                unbind(cssEditor.initializedProperty(), fxmlEditor.initializedProperty());
            }

            @Override
            protected boolean computeValue() {
                final boolean cssReady = cssEditor.isInitialized();
                final boolean fxmlReady = fxmlEditor.isInitialized();
                return cssReady && fxmlReady;
            }
        };
        codeEditorInitialized.addListener((final ObservableValue<? extends Boolean> observableValue, final Boolean oldValue, final Boolean newValue) -> {
            if (newValue) {
                try {
                    cssEditor.setMode(CodeEditor.Mode.CSS);
                    fxmlEditor.setMode(CodeEditor.Mode.XML);
                    reloadCSSFromTemplate();
                    reloadFXMLFromTemplate();
                    populatePreviewPane();
                    changePreviewLabels();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                } finally {
                    cssEditor.textProperty().addListener(textInvalitationListener);
                    fxmlEditor.textProperty().addListener(textInvalitationListener);
                    codeEditorInitialized.dispose();
                    codeEditorInitialized = null;
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever selection in the preview combo changes.
     */
    private final InvalidationListener previewSelectionInvalidationListener = (Observable observable) -> changePreviewLabels();

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

    /**
     * Called whenever the text in one of the editor has been modified.
     */
    private final InvalidationListener textInvalitationListener = (Observable observable) -> requestSaveAndReload();

    ////////////////////////////////////////////////////////////////////////////    
    /**
     * After loading infections, create new columns in the table for each state.
     */
    private void populateStatesColumns() {
        states.forEach((final State state) -> {
            final TableColumn<Infection, Boolean> stateColumn = new TableColumn<>(state.getName());
            stateColumn.setEditable(true);
            stateColumn.setCellValueFactory((TableColumn.CellDataFeatures<Infection, Boolean> p) -> {
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
            stateColumn.setCellFactory(CheckBoxTableCell.forTableColumn(stateColumn));
            infectionTable.getColumns().add(stateColumn);
        });
    }

    /**
     * Repopulate the preview pane.
     */
    private void populatePreviewPane() {
        // Remove old previews.
        previewPane.getChildren().clear();
        try {
            // Try to load the node to see if it works or not.
            final URL fxmlURL = fxmlFile.toURI().toURL();
            final URL cssURL = cssFile.toURI().toURL();
            final File tempCSSFile = File.createTempFile(cssFile.getName(), null);
            try (final FileOutputStream tempCSSOutput = new FileOutputStream(tempCSSFile)) {
                Files.copy(cssFile.toPath(), tempCSSOutput);
            }
            final URL tempCSSURL = tempCSSFile.toURI().toURL();
            final FXMLLoader fxmlLoader = new FXMLLoader(fxmlURL);
            final Region node = fxmlLoader.load();
//            node.getStylesheets().add(cssURL.toExternalForm());
            node.getStylesheets().add(tempCSSURL.toExternalForm());
            // Load new previews.
            states.forEach((final State state) -> {
                try {
                    final FXMLLoader fxmlLoader1 = new FXMLLoader(fxmlURL);
                    final Region node1 = fxmlLoader1.load();
//                    node1.getStylesheets().add(cssURL.toExternalForm());
                    node1.getStylesheets().add(tempCSSURL.toExternalForm());
                    final PseudoClass pseudoClass = PseudoClass.getPseudoClass(state.getName());
                    node1.pseudoClassStateChanged(pseudoClass, true);
                    final Group group = new Group(node1);
                    final StackPane pane = new StackPane(group);
                    VBox.setVgrow(pane, Priority.ALWAYS);
                    previewPane.getChildren().add(pane);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void changePreviewLabels() {
        Platform.runLater(() -> {
            final Infection infection = previewCombo.getValue();
            final String text = (infection == null) ? I18N.getString("LABEL_LABEL") : infection.getName(); // NOI18N.
            final Set<Node> allLabels = previewPane.lookupAll(".label"); // NOI18N.
            allLabels.stream().map((Node node) -> (Label) node).forEach((Label label) -> label.setText(text));
        });
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the default button of the preview pane is clicked.
     */
    @FXML
    private void handlePreviewDefaultButton(final ActionEvent actionEvent) {
        previewCombo.setValue(null);
    }

    /**
     * Called whenever the browse button of the target folder is clicked.
     */
    @FXML
    private void handleTargetBrowseButton(final ActionEvent actionEvent) {
        final String path = Settings.getPrefs().get("last.output.folder", System.getProperty("user.home")); // NOI18N.
        File folder = new File(path);
        folder = (!folder.exists() || !folder.isDirectory()) ? new File(System.getProperty("user.home")) : folder; // NOI18N.
        final DirectoryChooser dialog = new DirectoryChooser();
        dialog.setInitialDirectory(folder);
        folder = dialog.showDialog(previewPane.getScene().getWindow());
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
     * Called whenever the refresh button is clicked.
     */
    @FXML
    private void handleRefreshButton(final ActionEvent actionEvent) {
        populatePreviewPane();
        changePreviewLabels();
    }

    /**
     * The service that generates the images.
     */
    private Service<Void> generationService;

    /**
     * Called whenever the generate button is clicked.
     */
    @FXML
    private void handleGenerateButton(final ActionEvent actionEvent) {
        if (generationService != null) {
            generationService.cancel();
        } else {
            generationService = new Service<Void>() {

                @Override
                protected Task<Void> createTask() {
                    // Output folder.
                    final String path = Settings.getPrefs().get("last.output.folder", System.getProperty("user.home")); // NOI18N.
                    final File folder = new File(path);
                    // Copy infection list.
                    final List<Infection> infectionList = new LinkedList<>(infections);
                    return new GenerationTask(folder, infectionList, fxmlFile, cssFile);
                }
            };
            generationService.setOnSucceeded((final WorkerStateEvent workerStateEvent) -> {
            });
            generationService.setOnCancelled((final WorkerStateEvent workerStateEvent) -> {
            });
            generationService.setOnFailed((final WorkerStateEvent workerStateEvent) -> {
                final Throwable ex = generationService.getException();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            });
            generateProgressBar.progressProperty().bind(generationService.progressProperty());
        }
        generationService.restart();
    }

    /**
     * Called whenever the CSS default button is clicked.
     */
    @FXML
    private void handleCSSDefaultButton(final ActionEvent actionEvent) {
        try {
            exportCSSFromSource();
            reloadCSSFromTemplate();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Called whenever the CSS link is clicked.
     */
    @FXML
    private void handleCSSLink(final ActionEvent actionEvent) {
        final Optional<Application> application = Optional.ofNullable(getApplication());
        application.ifPresent((Application app) -> {
            // @todo get URL from properties.
            final String url = "http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html";
            app.getHostServices().showDocument(url);
        });
    }

    /**
     * Called whenever the FXML default button is clicked.
     */
    @FXML
    private void handleFXMLDefaultButton(final ActionEvent actionEvent) {
        try {
            exportFXMLFromSource();
            reloadFXMLFromTemplate();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private void exportCSSFromSource() throws IOException {
        final URL url = getClass().getResource("template/template.css"); // NOI18N.
        exportSourceToTemplate(url, cssFile);
    }

    private void exportFXMLFromSource() throws IOException {
        final URL url = getClass().getResource("template/template.fxml"); // NOI18N.
        exportSourceToTemplate(url, fxmlFile);
    }

    private void exportInfectionsFromSource() throws IOException {
        final URL url = getClass().getResource("template/infections.properties"); // NOI18N.
        exportSourceToTemplate(url, infectionsFile);
    }

    private void exportStatesFromSource() throws IOException {
        final URL url = getClass().getResource("template/states.properties"); // NOI18N.
        exportSourceToTemplate(url, statesFile);
    }

    private void exportSourceToTemplate(final URL url, final File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        try (final InputStream input = url.openStream()) {
            Files.copy(input, file.toPath());
        }
    }

    private void reloadCSSFromTemplate() throws IOException {
        reloadTextFromTemplate(cssFile, cssEditor);
    }

    private void reloadFXMLFromTemplate() throws IOException {
        reloadTextFromTemplate(fxmlFile, fxmlEditor);
    }

    private void reloadTextFromTemplate(final File file, final CodeEditor editor) throws IOException {
        try (final FileReader fileReader = new FileReader(file)) {
            try (final LineNumberReader lineReader = new LineNumberReader(fileReader)) {
                final StringBuilder builder = new StringBuilder();
                for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
                    builder.append(line);
                    builder.append("\n");
                }
                editor.setText(builder.toString());
            }
        }
    }

    private void reloadStatesFromTemplate() throws IOException {
        final Properties fileContent = new Properties();
        try (final FileInputStream input = new FileInputStream(statesFile)) {
            fileContent.load(input);
        }
        final List<String> values = new ArrayList<>(fileContent.stringPropertyNames());
        Collections.sort(values);
        values.stream().map((value) -> {
            final String color = fileContent.getProperty(value);
            final State state = initializeState(value, color);
            return state;
        }).forEach((state) -> states.add(state));
    }

    private State initializeState(final String name, final String colorName) {
        final Color color = (colorName == null || colorName.trim().isEmpty()) ? Color.BLACK : Color.valueOf(colorName);
        final State state = new State(name, color);
        return state;
    }

    private void reloadInfectionsFromTemplate() throws IOException {
        final Properties fileContent = new Properties();
        try (final FileInputStream input = new FileInputStream(infectionsFile)) {
            fileContent.load(input);
        }
        final List<String> values = new ArrayList<>(fileContent.stringPropertyNames());
        Collections.sort(values);
        values.forEach((final String value) -> {
            final String name = value.replaceAll("_", " "); // NOI18N.
            final String line = fileContent.getProperty(value);
            final String[] lineTokens = line.split("\\|"); // NOI18N.
            final String fileName = lineTokens.length == 1 ? null : lineTokens[0];
            final Infection infection = new Infection(name, fileName);
            final String statesLine = lineTokens.length == 1 ? lineTokens[0] : lineTokens[1];
            final String[] tokens = statesLine.split("\\s+"); // NOI18N.
            for (final String token : tokens) {
                final FilteredList<State> filteredStates = states.filtered((state) -> token.equals(state.getName()));
                if (filteredStates.isEmpty()) {
                    final State state = initializeState(token, null);
                    states.add(state);
                }
                infection.getStates().add(filteredStates.get(0));
            }
            infections.add(infection);
        });
    }

    private void saveCSSToTemplate() throws IOException {
        saveToTemplate(cssEditor, cssFile);
    }

    private void saveFXMLToTemplate() throws IOException {
        saveToTemplate(fxmlEditor, fxmlFile);
    }

    private void saveToTemplate(final CodeEditor editor, final File file) throws IOException {
        try (final PrintWriter writer = new PrintWriter(file)) {
            final String text = editor.getText();
            writer.println(text);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    private Optional<PauseTransition> waitTimer = Optional.empty();
    private final Duration timerDuration = Duration.millis(750);

    private void requestSaveAndReload() {
        System.out.println("requestSaveAndReload()");
        if (!waitTimer.isPresent()) {
            PauseTransition pauseTransition = new PauseTransition(timerDuration);
            pauseTransition.setOnFinished((final ActionEvent actionEvent) -> {
                waitTimer = Optional.empty();
                saveAndReload();
            });
            waitTimer = Optional.of(pauseTransition);
        }
        waitTimer.ifPresent((PauseTransition p) -> p.playFromStart());
    }

    /**
     * Save edited text and reload content of preview panel.
     */
    private void saveAndReload() {
        System.out.println("saveAndReload()");
        try {
            saveCSSToTemplate();
            saveFXMLToTemplate();
            populatePreviewPane();
            changePreviewLabels();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
