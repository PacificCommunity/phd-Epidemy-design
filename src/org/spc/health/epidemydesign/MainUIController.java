/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.spc.health.epidemydesign.control.codeeditor.CodeEditor;
import org.spc.health.epidemydesign.control.generatepane.GeneratePaneController;
import org.spc.health.epidemydesign.control.infectioneditor.InfectionEditorController;
import org.spc.health.epidemydesign.control.stateeditor.StateEditorController;
import org.spc.health.epidemydesign.task.GenerationTask;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The main UI controller.
 *
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class MainUIController extends ControllerBase implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MainUIController.class.getName());
    private static final String ENCODING = "UTF-8";
    private final File homeFolder;
    private final File templateFolder;
    private final File fxmlFile;
    private final File cssFile;
    private final File infectionsFile;
    private final File statesFile;
    private final ObservableList<State> states = FXCollections.observableList(new LinkedList<>());
    private final ObservableList<Infection> infections = FXCollections.observableList(new LinkedList<>());
    /**
     * Called whenever the state list of an infection changes content.
     */
    private final ListChangeListener<State> invalidationStateListChangeListener = _ -> Platform.runLater(() -> {
        try {
            saveInfectionsToTemplate();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    });
    private final Duration timerDuration = Duration.millis(750);
    @FXML
    private VBox cssContent;
    @FXML
    private VBox fxmlContent;
    @FXML
    private VBox previewPane;
    private final ListChangeListener<State> statesListChangeListener = (final Change<? extends State> _) -> Platform.runLater(() -> {
        try {
            saveStatesToTemplate();
            populatePreviewPane();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    });
    @FXML
    private ComboBox<Infection> previewCombo;
    ////////////////////////////////////////////////////////////////////////////
    private final ListChangeListener<Infection> infectionsListChangeListener = (final Change<? extends Infection> _) -> {
        final var comboList = new LinkedList<Infection>();
        comboList.add(null);
        comboList.addAll(infections);
        previewCombo.getItems().setAll(comboList);
        comboList.clear();
    };
    /**
     * Called whenever selection in the preview combo changes.
     */
    private final InvalidationListener previewSelectionInvalidationListener = (Observable _) -> Platform.runLater(this::changePreviewLabels);
    /**
     * Called whenever one of the text values of an infection changes.
     */
    private final InvalidationListener infectionValueInvalidationListener = (Observable _) -> Platform.runLater(() -> {
        try {
            saveInfectionsToTemplate();
            changePreviewLabels();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    });
    @FXML
    private SplitMenuButton loadCSSButton;
    @FXML
    private SplitMenuButton loadFXMLButton;
    @FXML
    private StateEditorController stateEditorController;
    @FXML
    private InfectionEditorController infectionEditorController;
    @FXML
    private GeneratePaneController generatePaneController;
    private CodeEditor cssEditor;
    private CodeEditor fxmlEditor;
    /**
     * This binding is used to control whenever the code editor has been initialized.
     * <br/>It's a member to avoid early GC.
     */
    private BooleanBinding codeEditorInitialized;

    ////////////////////////////////////////////////////////////////////////////    
    ////////////////////////////////////////////////////////////////////////////
    private PauseTransition waitTimer = null;
    /**
     * Called whenever the text in one of the editor has been modified.
     */
    private final InvalidationListener textInvalitationListener = _ -> requestSaveAndReload();

    ////////////////////////////////////////////////////////////////////////////
    /**
     * The service that generates the images.
     */
    private Service<Void> generationService;

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        infections.addListener(infectionsListChangeListener);
        states.addListener(statesListChangeListener);
        try {
            reloadStatesFromTemplate();
            reloadInfectionsFromTemplate();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        //
        stateEditorController.applicationProperty().bind(applicationProperty());
        stateEditorController.setStates(states);
        stateEditorController.setOnSave(_ -> saveStatesMayBe());
        stateEditorController.setOnLoad(_ -> importStatesMayBe());
        stateEditorController.setOnDefault(_ -> {
            try {
                exportStatesFromSource();
                reloadStatesFromTemplate();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        stateEditorController.setOnSelectFile((final File file) -> {
            try {
                clearStates();
                reloadStatesFromFile(file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        //
        infectionEditorController.applicationProperty().bind(applicationProperty());
        infectionEditorController.setInfections(infections);
        infectionEditorController.setStates(states);
        infectionEditorController.setOnSave(_ -> saveInfectionsMayBe());
        infectionEditorController.setOnLoad(_ -> importInfectionsMayBe());
        infectionEditorController.setOnDefault(_ -> {
            try {
                exportInfectionsFromSource();
                reloadInfectionsFromTemplate();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        infectionEditorController.setOnSelectFile(file -> {
            try {
                clearInfections();
                reloadInfectionsFromFile(file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        //
        //
        generatePaneController.applicationProperty().bind(applicationProperty());
        generatePaneController.setOnGenerate(_ -> generateOutput());
        //
        previewCombo.valueProperty().addListener(previewSelectionInvalidationListener);
        previewCombo.setButtonCell(new InfectionListCell());
        previewCombo.setCellFactory(_ -> new InfectionListCell());
        previewCombo.setValue(null);
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
        codeEditorInitialized.addListener((_, _, newValue) -> {
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

    /**
     * Repopulate the preview pane.
     */
    private void populatePreviewPane() {
        // Remove old previews.
        previewPane.getChildren().clear();
        try {
            // Try to load the node to see if it works or not.
            final var fxmlURL = fxmlFile.toURI().toURL();
            final var cssURL = cssFile.toURI().toURL();
            final var tempCSSFile = File.createTempFile(cssFile.getName(), null);
            try (final var tempCSSOutput = new FileOutputStream(tempCSSFile)) {
                Files.copy(cssFile.toPath(), tempCSSOutput);
            }
            final var tempCSSURL = tempCSSFile.toURI().toURL();
            final var fxmlLoader = new FXMLLoader(fxmlURL);
            final var node = fxmlLoader.<Region>load();
//            node.getStylesheets().add(cssURL.toExternalForm());
            node.getStylesheets().add(tempCSSURL.toExternalForm());
            // Load new previews.
            states.forEach((final var state) -> {
                try {
                    final var fxmlLoader1 = new FXMLLoader(fxmlURL);
                    final var node1 = fxmlLoader1.<Region>load();
//                    node1.getStylesheets().add(cssURL.toExternalForm());
                    node1.getStylesheets().add(tempCSSURL.toExternalForm());
                    final var pseudoClass = PseudoClass.getPseudoClass(state.getName());
                    node1.pseudoClassStateChanged(pseudoClass, true);
                    final var stateGroup = new Group(node1);
                    stateGroup.setId("stateGroup_%s".formatted(state)); // NOI18N.
                    StackPane statePreviewPane = new StackPane(stateGroup);
                    statePreviewPane.getStyleClass().add("preview-pane"); // NOI18N.
                    final var stateLabel = new Label();
                    stateLabel.setId("stateLabel_%s".formatted(state)); // NOI18N.
                    stateLabel.getStyleClass().add("state-label");
                    stateLabel.setText(state.getName());
                    final var stateActionBar = new HBox();
                    stateActionBar.getStyleClass().add("action-bar"); // NOI18N.
                    stateActionBar.getChildren().add(stateLabel);
                    final var previewThumbnail = new BorderPane();
                    previewThumbnail.getStyleClass().add("preview-thumbnail"); // NOI18N.
                    previewThumbnail.setTop(stateActionBar);
                    previewThumbnail.setCenter(statePreviewPane);
                    VBox.setVgrow(previewThumbnail, Priority.ALWAYS);
                    previewPane.getChildren().add(previewThumbnail);
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
            final var infection = previewCombo.getValue();
            final var text = (infection == null) ? I18N.getString("label.label") : infection.getName(); // NOI18N.
            final var allLabels = previewPane.lookupAll(".label"); // NOI18N.
            allLabels.stream()
                    .map(node -> (Label) node)
                    .forEach(label -> {
                        if (!label.getId().contains("stateLabel")) {
                            label.setText(text);
                        }
                    });
        });
    }

    /**
     * Called whenever the default button of the preview pane is clicked.
     */
    @FXML
    private void handlePreviewDefaultButton(final ActionEvent actionEvent) {
        previewCombo.setValue(null);
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
        Optional.ofNullable(getApplication())
                .ifPresent(app -> {
                    // @todo get URL from properties.
                    final String url = "http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html";
                    app.getHostServices().showDocument(url);
                });
    }

    ////////////////////////////////////////////////////////////////////////////

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

    @FXML
    private void handleSaveCSSButton(final ActionEvent actionEvent) {
        exportTemplateMayBe(cssEditor, "css", loadCSSButton); // NOI18N.
    }

    @FXML
    private void handleSaveFXMLButton(final ActionEvent actionEvent) {
        exportTemplateMayBe(fxmlEditor, "fxml", loadFXMLButton); // NOI18N.
    }

    @FXML
    private void handleLoadCSSButton(final ActionEvent actionEvent) {
        importTemplateMayBe(cssEditor, "css", loadCSSButton); // NOI18N.
    }

    @FXML
    private void handleLoadFXMLButton(final ActionEvent actionEvent) {
        importTemplateMayBe(fxmlEditor, "fxml", loadFXMLButton); // NOI18N.
    }

    /**
     * Display a file dialog box that allows the user to export a template.
     *
     * @param codeEditor Source code editor.
     * @param extension  File extension to use.
     * @param loadButton The associated load button.
     */
    private void exportTemplateMayBe(final CodeEditor codeEditor, final String extension, final SplitMenuButton loadButton) {
        final var dialog = prepareInputFileDialog("template", extension);
        Optional.ofNullable(dialog.showSaveDialog(loadButton.getScene().getWindow()))
                .ifPresent(file -> {
                    Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
                    try {
                        exportTemplateToFile(codeEditor, file);
                        addFileToLoadButton(codeEditor, loadButton, file);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Prepare the file dialog for input files.
     *
     * @param prefix    File prefix to be used.
     * @param extension File extension to be used.
     * @return A {@code FileChooser} instance, never {@code null}.
     */
    private FileChooser prepareInputFileDialog(final String prefix, final String extension) {
        final var userHome = System.getProperty("user.home"); // NOI18N.
        final var path = Settings.getPrefs().get("last.input.folder", userHome); // NOI18N.
        var folder = new File(path);
        folder = (!folder.exists()) ? new File(userHome) : folder;
        final var dialog = new FileChooser();
        final var allDescription = I18N.getString("all-files.label"); // NOI18N.
        final var allExtension = String.format(I18N.getString("extension-xx.template"), "*"); // NOI18N.
        final var allFilter = new FileChooser.ExtensionFilter(allDescription, allExtension);
        final var extensionDescription = String.format(I18N.getString("file-xx.template"), extension.toUpperCase()); // NOI18N.
        final var extensionExtension = String.format(I18N.getString("extension-xx.template"), extension); // NOI18N.
        final var extensionFilter = new FileChooser.ExtensionFilter(extensionDescription, extensionExtension);
        dialog.getExtensionFilters().setAll(extensionFilter, allFilter);
        dialog.setSelectedExtensionFilter(extensionFilter);
        dialog.setInitialDirectory(folder);
        dialog.setInitialFileName(String.format("%s.%s", prefix, extension)); // NOI18N.
        return dialog;
    }

    /**
     * Add selected file to the load button's menu.
     *
     * @param codeEditor The code editor (used when activating the menu item).
     * @param loadButton The load button which will host the menu.
     * @param file       The source file.
     */
    private void addFileToLoadButton(final CodeEditor codeEditor, final SplitMenuButton loadButton, final File file) {
        boolean found = false;
        for (final var menuItem : loadButton.getItems()) {
            final var itemFile = (File) menuItem.getProperties().get("file"); // NOI18N.
            if (file.equals(itemFile)) {
                found = true;
                break;
            }
        }
        if (!found) {
            final var menuItem = new MenuItem(file.getAbsolutePath());
            menuItem.getProperties().put("file", file); // NOI18N.
            menuItem.setOnAction(_ -> {
                try {
                    reloadTextFromTemplate(file, codeEditor);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
            loadButton.getItems().add(menuItem);
        }
    }

    /**
     * Save current template into selected file.
     *
     * @param editor The source editor.
     * @param file   The target file.
     * @throws IOException In case of IO error.
     */
    private void exportTemplateToFile(final CodeEditor editor, final File file) throws IOException {
        final var text = editor.getText();
        try (final var printWriter = new PrintWriter(file, ENCODING)) {
            printWriter.print(text);
        }
    }

    /**
     * Display a file dialog box that allows the user to import a template.
     *
     * @param codeEditor Source code editor.
     * @param extension  File extension to use.
     * @param loadButton The associated load button.
     */
    private void importTemplateMayBe(final CodeEditor codeEditor, final String extension, final SplitMenuButton loadButton) {
        final var dialog = prepareInputFileDialog("template", extension);
        Optional.ofNullable(dialog.showOpenDialog(loadButton.getScene().getWindow()))
                .ifPresent(file -> {
                    Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
                    try {
                        reloadTextFromTemplate(file, codeEditor);
                        addFileToLoadButton(codeEditor, loadButton, file);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
    }

    private void exportCSSFromSource() throws IOException {
        final var url = getClass().getResource("template/template.css"); // NOI18N.
        exportSourceToTemplate(url, cssFile);
    }

    private void exportFXMLFromSource() throws IOException {
        final var url = getClass().getResource("template/template.fxml"); // NOI18N.
        exportSourceToTemplate(url, fxmlFile);
    }

    private void exportInfectionsFromSource() throws IOException {
        final var url = getClass().getResource("template/infections.properties"); // NOI18N.
        exportSourceToTemplate(url, infectionsFile);
    }

    private void exportStatesFromSource() throws IOException {
        final var url = getClass().getResource("template/states.properties"); // NOI18N.
        exportSourceToTemplate(url, statesFile);
    }

    private void exportSourceToTemplate(final URL url, final File file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        try (final var input = url.openStream()) {
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
        try (final var fileReader = new FileReader(file)) {
            try (final var lineReader = new LineNumberReader(fileReader)) {
                final var builder = new StringBuilder();
                for (var line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
                    builder.append(line);
                    builder.append("\n");
                }
                editor.setText(builder.toString());
            }
        }
    }

    private void clearStates() {
        states.clear();
    }

    private void reloadStatesFromTemplate() throws IOException {
        clearStates();
        reloadStatesFromFile(statesFile);
    }

    private void reloadStatesFromFile(final File file) throws IOException {
        final var fileContent = new Properties();
        try (final var input = new FileInputStream(file)) {
            fileContent.load(input);
        }
        final var values = new ArrayList<>(fileContent.stringPropertyNames());
        Collections.sort(values);
        values.stream()
                .map(value -> {
                    final var color = fileContent.getProperty(value);
                    return initializeState(value, color);
                })
                .forEach(states::add);
        Collections.sort(states);
    }

    private State initializeState(final String name, final String colorName) {
        final var color = (Objects.isNull(colorName) || colorName.isBlank()) ? Color.BLACK : Color.valueOf(colorName);
        return new State(name, color);
    }

    private void reloadInfectionsFromTemplate() throws IOException {
        clearInfections();
        reloadInfectionsFromFile(infectionsFile);
    }

    private void clearInfections() {
        infections.forEach(infection -> {
            infection.nameProperty().removeListener(infectionValueInvalidationListener);
            infection.fileNameProperty().removeListener(infectionValueInvalidationListener);
            infection.getStates().removeListener(invalidationStateListChangeListener);
        });
        infections.clear();
    }

    private void reloadInfectionsFromFile(final File file) throws IOException {
        final var fileContent = new Properties();
        try (final var input = new FileInputStream(file)) {
            fileContent.load(input);
        }
        final var values = new ArrayList<>(fileContent.stringPropertyNames());
        Collections.sort(values);
        values.forEach(value -> {
            final var name = value.replaceAll("_", " "); // NOI18N.
            final var line = fileContent.getProperty(value);
            final var lineTokens = line.split("\\|"); // NOI18N.
            final var fileName = (line.contains("|") || lineTokens.length > 1) ? lineTokens[0] : null; // NOI18N.
            final var infection = new Infection(name, fileName);
            final var statesLine = (lineTokens.length == 1) ? (line.contains("|") ? "" : lineTokens[0]) : lineTokens[1]; // NOI18N.
            final var tokens = statesLine.split("\\s+"); // NOI18N.
            for (final var token : tokens) {
                final var stateName = token.trim();
                if (stateName.isEmpty()) {
                    continue;
                }
                final var filteredStates = states.filtered((state) -> token.equals(state.getName()));
                if (filteredStates.isEmpty()) {
                    final var state = initializeState(token, null);
                    states.add(state);
                }
                infection.getStates().add(filteredStates.getFirst());
            }
            infection.nameProperty().addListener(infectionValueInvalidationListener);
            infection.fileNameProperty().addListener(infectionValueInvalidationListener);
            infection.getStates().addListener(invalidationStateListChangeListener);
            infections.add(infection);
        });
        Collections.sort(states);
        Collections.sort(infections);
    }

    private void saveCSSToTemplate() throws IOException {
        saveCodeToFile(cssEditor, cssFile);
    }

    private void saveFXMLToTemplate() throws IOException {
        saveCodeToFile(fxmlEditor, fxmlFile);
    }

    private void saveCodeToFile(final CodeEditor editor, final File file) throws IOException {
        try (final var writer = new PrintWriter(file, ENCODING)) {
            final var text = editor.getText();
            writer.println(text);
        }
    }

    private void importStatesMayBe() {
        final var dialog = prepareInputFileDialog("states", "properties");
        Optional.ofNullable(dialog.showOpenDialog(loadCSSButton.getScene().getWindow()))
                .ifPresent(file -> {
                    Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
                    try {
                        clearStates();
                        reloadStatesFromFile(file);
                        if (!stateEditorController.getRecentFiles().contains(file)) {
                            stateEditorController.getRecentFiles().add(file);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
    }

    private void saveStatesMayBe() {
        final var dialog = prepareInputFileDialog("states", "properties"); // NOI18N.
        Optional.ofNullable(dialog.showSaveDialog(loadCSSButton.getScene().getWindow()))
                .ifPresent(file -> {
                    Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
                    try {
                        saveStatesToFile(file);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
    }

    private void saveStatesToTemplate() throws IOException {
        saveStatesToFile(statesFile);
    }

    private void saveStatesToFile(final File file) throws IOException {
        try (final var writer = new PrintWriter(file, ENCODING)) {
            states.forEach((State state) -> {
                final var line = new StringBuilder();
                final var name = state.getName();
                line.append(name);
                line.append("="); // NOI18N.
                final var color = state.getColor();
                if (Objects.nonNull(color)) {
                    final var webColor = String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255)); // NOI18N.
                    line.append(webColor);
                }
                writer.println(line.toString().trim());
            });
        }
    }

    private void importInfectionsMayBe() {
        final var dialog = prepareInputFileDialog("infections", "properties");
        Optional.ofNullable(dialog.showOpenDialog(loadCSSButton.getScene().getWindow()))
                .ifPresent(file -> {
                    Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
                    try {
                        clearInfections();
                        reloadInfectionsFromFile(file);
                        if (!infectionEditorController.getRecentFiles().contains(file)) {
                            infectionEditorController.getRecentFiles().add(file);
                        }
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
    }

    private void saveInfectionsMayBe() {
        final var dialog = prepareInputFileDialog("infections", "properties"); // NOI18N.
        Optional.ofNullable(dialog.showSaveDialog(loadCSSButton.getScene().getWindow()))
                .ifPresent(file -> {
                    Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
                    try {
                        saveInfectionsToFile(file);
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                });
    }

    private void saveInfectionsToTemplate() throws IOException {
        saveInfectionsToFile(infectionsFile);
    }

    private void saveInfectionsToFile(final File file) throws IOException {
        try (final var writer = new PrintWriter(file, ENCODING)) {
            infections.forEach((Infection infection) -> {
                final var line = new StringBuilder();
                final var name = infection.getName();
                line.append(name);
                line.append("="); // NOI18N.
                final var fileName = infection.getFileName();
                if (Objects.nonNull(fileName) && !fileName.isBlank()) {
                    line.append(fileName);
                    line.append("|"); // NOI18N.
                }
                infection.getStates().forEach(state -> {
                    line.append(state);
                    line.append(" "); // NOI18N.
                });
                writer.println(line.toString().trim());
            });
        }
    }

    private void requestSaveAndReload() {
        LOGGER.log(Level.INFO, "requestSaveAndReload()");
        if (Objects.isNull(waitTimer)) {
            final var pauseTransition = new PauseTransition(timerDuration);
            pauseTransition.setOnFinished(_ -> {
                waitTimer = null;
                saveAndReload();
            });
            waitTimer = pauseTransition;
        }
        waitTimer.playFromStart();
    }

    /**
     * Save edited text and reload content of preview panel.
     */
    private void saveAndReload() {
        LOGGER.log(Level.INFO, "saveAndReload()");
        try {
            saveCSSToTemplate();
            saveFXMLToTemplate();
            saveInfectionsToTemplate();
            populatePreviewPane();
            changePreviewLabels();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private void generateOutput() {
        Optional.ofNullable(generationService)
                .ifPresent(Service::cancel);
        if (Objects.isNull(generationService)) {
            generatePaneController.setProgress(0);
            generationService = new Service<>() {

                @Override
                protected Task<Void> createTask() {
                    // Output folder.
                    final var userHome = System.getProperty("user.home"); // NOI18N.
                    final var path = Settings.getPrefs().get("last.output.folder", userHome); // NOI18N.
                    final var folder = new File(path);
                    // Copy infection list.
                    final var infectionList = new LinkedList<>(infections);
                    return new GenerationTask(folder, infectionList, fxmlFile, cssFile);
                }
            };
            generationService.setOnSucceeded(_ -> LOGGER.log(Level.INFO, "Output generation succeeded."));
            generationService.setOnCancelled(_ -> LOGGER.log(Level.INFO, "Output generation canceled."));
            generationService.setOnFailed(_ -> {
                final var ex = generationService.getException();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            });
            generatePaneController.progressProperty().bind(generationService.progressProperty());
        }
        generationService.restart();
    }
}
