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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.spc.health.epidemydesign.control.codeeditor.CodeEditor;
import org.spc.health.epidemydesign.control.generatepane.GeneratePaneController;
import org.spc.health.epidemydesign.control.infectioneditor.InfectionEditorController;
import org.spc.health.epidemydesign.task.GenerationTask;

/**
 * The main UI controller.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class MainUIController extends ControllerBase implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(MainUIController.class.getName());
    private static final String ENCODING = "UTF-8";

    @FXML
    private VBox cssContent;
    @FXML
    private VBox fxmlContent;
    @FXML
    private VBox previewPane;
    @FXML
    private ComboBox<Infection> previewCombo;
    @FXML
    private SplitMenuButton loadCSSButton;
    @FXML
    private SplitMenuButton loadFXMLButton;
    @FXML
    private InfectionEditorController infectionEditorController;
    @FXML
    private GeneratePaneController generatePaneController;

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
     * <br/>It's a member in order to avoid early GC.
     */
    private BooleanBinding codeEditorInitialized;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        infections.addListener((final ListChangeListener.Change<? extends Infection> change) -> {
            final List<Infection> comboList = new LinkedList<>();
            comboList.add(null);
            comboList.addAll(infections);
            previewCombo.getItems().setAll(comboList);
            comboList.clear();
        });
        try {
            reloadStatesFromTemplate();
            reloadInfectionsFromTemplate();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        //
        infectionEditorController.applicationProperty().bind(applicationProperty());
        infectionEditorController.setInfections(infections);
        infectionEditorController.setStates(states);
        infectionEditorController.setOnInfectionSave((final ActionEvent actionEvent) -> saveInfectionsMayBe());
        infectionEditorController.setOnInfectionLoad((final ActionEvent actionEvent) -> importInfectionsMayBe());
        infectionEditorController.setOnInfectionDefault((final ActionEvent actionEvent) -> {
            try {
                exportInfectionsFromSource();
                reloadInfectionsFromTemplate();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        infectionEditorController.setOnInfectionFile((final File file) -> {
            try {
                clearInfections();
                reloadInfectionsFromFile(file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
        //
        generatePaneController.setOnGenerate((final ActionEvent actionEvent) -> generateOutput());
        //
        previewCombo.valueProperty().addListener(previewSelectionInvalidationListener);
        previewCombo.setButtonCell(new InfectionListCell());
        previewCombo.setCellFactory((final ListView<Infection> listView) -> new InfectionListCell());
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
    private final InvalidationListener previewSelectionInvalidationListener = (Observable observable) -> {
        Platform.runLater(() -> {
            changePreviewLabels();
        });
    };

    /**
     * Called whenever the text in one of the editor has been modified.
     */
    private final InvalidationListener textInvalitationListener = (Observable observable) -> requestSaveAndReload();

    ////////////////////////////////////////////////////////////////////////////    
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

    @FXML
    private void handleAddInfectionsButton(final ActionEvent actionEvent) {
    }

    @FXML
    private void handleDeleteInfectionsButton(final ActionEvent actionEvent) {
    }

    @FXML
    private void handleSaveInfectionsButton(final ActionEvent actionEvent) {
    }

    @FXML
    private void handleLoadInfectionsButton(final ActionEvent actionEvent) {
    }

    @FXML
    private void handleInfectionsDefaultButton(final ActionEvent actionEvent) {
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Display a file dialog box that allows the user to export a template.
     * @param codeEditor Source code editor.
     * @param extension File extension to use.
     * @param loadButton The associated load button.
     */
    private void exportTemplateMayBe(final CodeEditor codeEditor, final String extension, final SplitMenuButton loadButton) {
        final FileChooser dialog = prepareInputFileDialog("template", extension);
        final File file = dialog.showSaveDialog(loadButton.getScene().getWindow());
        if (file != null) {
            Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
            try {
                exportTemplateToFile(codeEditor, file);
                addFileToLoadButton(codeEditor, loadButton, file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    /**
     * Prepare the file dialog for input files.
     * @param prefix File prefix to be used.
     * @param extension File extension to be used.
     * @return A {@code FileChooser} instance, never {@code null}.
     */
    private FileChooser prepareInputFileDialog(final String prefix, final String extension) {
        final String userHome = System.getProperty("user.home"); // NOI18N.
        final String path = Settings.getPrefs().get("last.input.folder", userHome); // NOI18N.
        File folder = new File(path);
        folder = (!folder.exists()) ? new File(userHome) : folder;
        final FileChooser dialog = new FileChooser();
        final String allDescription = I18N.getString("ALL_FILES_LABEL"); // NOI18N.
        final String allExtension = String.format(I18N.getString("EXTENSION_XX_TEMPLATE"), "*"); // NOI18N.
        final FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(allDescription, allExtension);
        final String extensionDescription = String.format(I18N.getString("FILE_XX_TEMPLATE"), extension.toUpperCase()); // NOI18N.
        final String extensionExtension = String.format(I18N.getString("EXTENSION_XX_TEMPLATE"), extension); // NOI18N.
        final FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter(extensionDescription, extensionExtension);
        dialog.getExtensionFilters().setAll(extensionFilter, allFilter);
        dialog.setSelectedExtensionFilter(extensionFilter);
        dialog.setInitialDirectory(folder);
        dialog.setInitialFileName(String.format("%s.%s", prefix, extension)); // NOI18N.
        return dialog;
    }

    /**
     * Add selected file to the load button's menu.
     * @param codeEditor The code editor (used when activating the menu item).
     * @param loadButton The load button which will host the menu.
     * @param file The source file.
     */
    private void addFileToLoadButton(final CodeEditor codeEditor, final SplitMenuButton loadButton, final File file) {
        boolean found = false;
        for (final MenuItem menuItem : loadButton.getItems()) {
            final File itemFile = (File) menuItem.getProperties().get("file"); // NOI18N.
            if (file.equals(itemFile)) {
                found = true;
                break;
            }
        }
        if (!found) {
            final MenuItem menuItem = new MenuItem(file.getAbsolutePath());
            menuItem.getProperties().put("file", file); // NOI18N.
            menuItem.setOnAction((final ActionEvent actionEvent) -> {
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
     * @param editor The source editor.
     * @param file The target file.
     * @throws IOException In case of IO error.
     */
    private void exportTemplateToFile(final CodeEditor editor, final File file) throws IOException {
        final String text = editor.getText();
        try (PrintWriter printWriter = new PrintWriter(file, ENCODING)) {
            printWriter.print(text);
        }
    }

    /**
     * Display a file dialog box that allows the user to import a template.
     * @param codeEditor Source code editor.
     * @param extension File extension to use.
     * @param loadButton The associated load button.
     */
    private void importTemplateMayBe(final CodeEditor codeEditor, final String extension, final SplitMenuButton loadButton) {
        final FileChooser dialog = prepareInputFileDialog("template", extension);
        final File file = dialog.showOpenDialog(loadButton.getScene().getWindow());
        if (file != null) {
            Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
            try {
                reloadTextFromTemplate(file, codeEditor);
                addFileToLoadButton(codeEditor, loadButton, file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

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
        clearInfections();
        reloadInfectionsFromFile(infectionsFile);
    }

    private void clearInfections() {
        infections.forEach((final Infection infection) -> {
            infection.nameProperty().removeListener(infectionValueInvalidationListener);
            infection.fileNameProperty().removeListener(infectionValueInvalidationListener);
            infection.getStates().removeListener(invalidationStateListChangeListener);
        });
        infections.clear();
    }

    private void reloadInfectionsFromFile(final File file) throws IOException {
        final Properties fileContent = new Properties();
        try (final FileInputStream input = new FileInputStream(file)) {
            fileContent.load(input);
        }
        final List<String> values = new ArrayList<>(fileContent.stringPropertyNames());
        Collections.sort(values);
        values.forEach((final String value) -> {
            final String name = value.replaceAll("_", " "); // NOI18N.
            final String line = fileContent.getProperty(value);
            final String[] lineTokens = line.split("\\|"); // NOI18N.
            final String fileName = (line.contains("|") || lineTokens.length > 1) ? lineTokens[0] : null; // NOI18N.
            final Infection infection = new Infection(name, fileName);
            final String statesLine = (lineTokens.length == 1) ? (line.contains("|") ? "" : lineTokens[0]) : lineTokens[1]; // NOI18N.
            final String[] tokens = statesLine.split("\\s+"); // NOI18N.
            for (final String token : tokens) {
                final String stateName = token.trim();
                if (stateName.isEmpty()) {
                    continue;
                }
                final FilteredList<State> filteredStates = states.filtered((state) -> token.equals(state.getName()));
                if (filteredStates.isEmpty()) {
                    final State state = initializeState(token, null);
                    states.add(state);
                }
                infection.getStates().add(filteredStates.get(0));
            }
            infection.nameProperty().addListener(infectionValueInvalidationListener);
            infection.fileNameProperty().addListener(infectionValueInvalidationListener);
            infection.getStates().addListener(invalidationStateListChangeListener);
            infections.add(infection);
        });
    }

    /**
     * Called whenever one of the text values of an infection changes.
     */
    private final InvalidationListener infectionValueInvalidationListener = (Observable observable) -> {
        Platform.runLater(() -> {
            try {
                saveInfectionsToTemplate();
                changePreviewLabels();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    };

    /**
     * Called whenever the state list of an infection changes content.
     */
    private final ListChangeListener<State> invalidationStateListChangeListener = (ListChangeListener.Change<? extends State> change) -> {
        Platform.runLater(() -> {
            try {
                saveInfectionsToTemplate();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    };

    private void saveCSSToTemplate() throws IOException {
        saveCodeToFile(cssEditor, cssFile);
    }

    private void saveFXMLToTemplate() throws IOException {
        saveCodeToFile(fxmlEditor, fxmlFile);
    }

    private void saveCodeToFile(final CodeEditor editor, final File file) throws IOException {
        try (final PrintWriter writer = new PrintWriter(file, ENCODING)) {
            final String text = editor.getText();
            writer.println(text);
        }
    }

    private void importInfectionsMayBe() {
        final FileChooser dialog = prepareInputFileDialog("infections", "properties");
        final File file = dialog.showOpenDialog(loadCSSButton.getScene().getWindow());
        if (file != null) {
            Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
            try {
                infections.clear();
                reloadInfectionsFromFile(file);
                if (!infectionEditorController.getRecentFiles().contains(file)) {
                    infectionEditorController.getRecentFiles().add(file);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private void saveInfectionsMayBe() {
        final FileChooser dialog = prepareInputFileDialog("infections", "properties"); // NOI18N.
        final File file = dialog.showSaveDialog(loadCSSButton.getScene().getWindow());
        if (file != null) {
            Settings.getPrefs().put("last.input.folder", file.getParent()); // NOI18N.
            try {
                saveInfectionsToFile(file);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private void saveInfectionsToTemplate() throws IOException {
        saveInfectionsToFile(infectionsFile);
    }

    private void saveInfectionsToFile(final File file) throws IOException {
        try (final PrintWriter writer = new PrintWriter(file, ENCODING)) {
            infections.forEach((Infection infection) -> {
                final StringBuilder line = new StringBuilder();
                final String name = infection.getName();
                line.append(name);
                line.append("="); // NOI18N.
                final String fileName = infection.getFileName();
                if (fileName != null && !fileName.trim().isEmpty()) {
                    line.append(fileName);
                    line.append("|"); // NOI18N.
                }
                infection.getStates().forEach((final State state) -> {
                    line.append(state);
                    line.append(" "); // NOI18N.
                });
                writer.println(line.toString().trim());
            });
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
            saveInfectionsToTemplate();
            populatePreviewPane();
            changePreviewLabels();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * The service that generates the images.
     */
    private Service<Void> generationService;

    private void generateOutput() {
        if (generationService != null) {
            generationService.cancel();
        } else {
            generatePaneController.setProgress(0);
            generationService = new Service<Void>() {

                @Override
                protected Task<Void> createTask() {
                    // Output folder.
                    String userHome = System.getProperty("user.home"); // NOI18N.
                    final String path = Settings.getPrefs().get("last.output.folder", userHome); // NOI18N.
                    final File folder = new File(path);
                    // Copy infection list.
                    final List<Infection> infectionList = new LinkedList<>(infections);
                    return new GenerationTask(folder, infectionList, fxmlFile, cssFile);
                }
            };
            generationService.setOnSucceeded((final WorkerStateEvent workerStateEvent) -> {
                LOGGER.log(Level.INFO, "Output generation succeeded.");
            });
            generationService.setOnCancelled((final WorkerStateEvent workerStateEvent) -> {
                LOGGER.log(Level.INFO, "Output generation canceled.");
            });
            generationService.setOnFailed((final WorkerStateEvent workerStateEvent) -> {
                final Throwable ex = generationService.getException();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            });
            generatePaneController.progressProperty().bind(generationService.progressProperty());
        }
        generationService.restart();
    }

}
