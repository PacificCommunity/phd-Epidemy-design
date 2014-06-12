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
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * The main UI controller.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class MainUIController extends ControllerBase implements Initializable {

    @FXML
    private TableView<Infection> infectionTable;
    @FXML
    private TableColumn<Infection, String> infectionColumn;
    @FXML
    private TextArea cssArea;
    @FXML
    private TextArea fxmlArea;
    @FXML
    private VBox previewPane;

    private final File homeFolder;
    private final File templateFolder;
    private final File fxmlFile;
    private final File cssFile;
    private final File infectionsFile;
    private final File statesFile;

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
        try {
            reloadFXMLFromTemplate();
            reloadCSSFromTemplate();
            reloadStatesFromTemplate();
            reloadInfectionsFromTemplate();
            System.out.println(states);
            System.out.println(infections);
            populatePreviewPane();
        } catch (IOException ex) {
            Logger.getLogger(MainUIController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        //
        infectionTable.setEditable(true);
        infectionTable.setItems(infections);
        infectionColumn.setCellValueFactory((TableColumn.CellDataFeatures<Infection, String> p) -> {
            final Infection infection = p.getValue();
            final String name = infection.name;
            return new SimpleStringProperty(name);
        });
        populateStatesColumns();
        //
        cssArea.textProperty().addListener(textInvalitationListener);
        fxmlArea.textProperty().addListener(textInvalitationListener);
    }

    private final InvalidationListener textInvalitationListener = (Observable observable) -> requestSaveAndReload();

    private void populateStatesColumns() {
        states.forEach((final State state) -> {
            final TableColumn<Infection, Boolean> stateColumn = new TableColumn<>(state.name);
            stateColumn.setEditable(true);
            stateColumn.setCellValueFactory((TableColumn.CellDataFeatures<Infection, Boolean> p) -> {
                final Infection infection = p.getValue();
                final boolean activated = infection.states.contains(state);
                final BooleanProperty property = new SimpleBooleanProperty(activated);
                property.addListener((ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) -> {
                    if (newValue) {
                        infection.states.add(state);
                    } else {
                        infection.states.remove(state);
                    }
                });
                return property;
            });
            stateColumn.setCellFactory(CheckBoxTableCell.forTableColumn(stateColumn));
            infectionTable.getColumns().add(stateColumn);
        });
    }

    private void populatePreviewPane() {
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
            // Remove old previews.
            previewPane.getChildren().clear();
            // Load new previews.
            states.forEach((final State state) -> {
                try {
                    final FXMLLoader fxmlLoader1 = new FXMLLoader(fxmlURL);
                    final Region node1 = fxmlLoader1.load();
//                    node1.getStylesheets().add(cssURL.toExternalForm());
                    node1.getStylesheets().add(tempCSSURL.toExternalForm());
                    final PseudoClass pseudoClass = PseudoClass.getPseudoClass(state.name);
                    node1.pseudoClassStateChanged(pseudoClass, true);
                    final Group group = new Group(node1);
                    final StackPane pane = new StackPane(group);
                    VBox.setVgrow(pane, Priority.ALWAYS);
                    previewPane.getChildren().add(pane);
                } catch (IOException ex) {
                    Logger.getLogger(MainUIController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(MainUIController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    /**
     * Called whenever the CSS default button is clicked.
     */
    @FXML
    private void handleCSSDefaultButton(ActionEvent event) {
        try {
            exportCSSFromSource();
            reloadCSSFromTemplate();
        } catch (IOException ex) {
            Logger.getLogger(MainUIController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Called whenever the CSS link is clicked.
     */
    @FXML
    private void handleCSSLink(ActionEvent event) {
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
    private void handleFXMLDefaultButton(ActionEvent event) {
        try {
            exportFXMLFromSource();
            reloadFXMLFromTemplate();
        } catch (IOException ex) {
            Logger.getLogger(MainUIController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
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
        reloadTextFromTemplate(cssFile, cssArea);
    }

    private void reloadFXMLFromTemplate() throws IOException {
        reloadTextFromTemplate(fxmlFile, fxmlArea);
    }

    private void reloadTextFromTemplate(final File file, final TextArea textArea) throws IOException {
        try (final FileReader fileReader = new FileReader(file)) {
            try (final LineNumberReader lineReader = new LineNumberReader(fileReader)) {
                final StringBuilder builder = new StringBuilder();
                for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
                    builder.append(line);
                    builder.append("\n");
                }
                textArea.setText(builder.toString());
            }
        }
    }

    private final ObservableList<State> states = FXCollections.observableList(new LinkedList<>());

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
        final State state = new State();
        state.name = name;
        state.color = (colorName == null || colorName.trim().isEmpty()) ? Color.BLACK : Color.valueOf(colorName);
        return state;
    }

    private final ObservableList<Infection> infections = FXCollections.observableList(new LinkedList<>());

    private void reloadInfectionsFromTemplate() throws IOException {
        final Properties fileContent = new Properties();
        try (final FileInputStream input = new FileInputStream(infectionsFile)) {
            fileContent.load(input);
        }
        final List<String> values = new ArrayList<>(fileContent.stringPropertyNames());
        Collections.sort(values);
        values.forEach((final String value) -> {
            final Infection infection = new Infection();
            final String name = value.replaceAll("_", " ");
            infection.name = name;
            final String statesLine = fileContent.getProperty(value);
            final String[] tokens = statesLine.split("\\s+"); // NOI18N.
            for (final String token : tokens) {
                final FilteredList<State> filteredStates = states.filtered((state) -> token.equals(state.name));
                if (filteredStates.isEmpty()) {
                    final State state = initializeState(token, null);
                    states.add(state);
                }
                infection.states.add(filteredStates.get(0));
            }
            infections.add(infection);
        });
    }

    private void saveCSSToTemplate() throws IOException {
        saveToTemplate(cssArea, cssFile);
    }

    private void saveFXMLToTemplate() throws IOException {
        saveToTemplate(fxmlArea, fxmlFile);
    }

    private void saveToTemplate(final TextArea textArea, final File file) throws IOException {
        try (final PrintWriter writer = new PrintWriter(file)) {
            final String text = textArea.getText();
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

    private void saveAndReload() {
        System.out.println("saveAndReload()");
        try {
            saveCSSToTemplate();
            saveFXMLToTemplate();
            populatePreviewPane();
        } catch (IOException ex) {
            Logger.getLogger(MainUIController.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
