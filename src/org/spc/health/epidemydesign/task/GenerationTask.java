/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign.task;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import org.spc.health.epidemydesign.Infection;

/**
 * Task that generates images.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public class GenerationTask extends Task<Void> {

    private final File folder;
    private final List<Infection> infections;
    private final File fxmlFile;
    private final File cssFile;
    private String format = "png"; // NOI18N.

    /**
     * Creates a new instance.
     * @param folder Target folder.
     * @param infections List of infections.
     * @param fxmlFile Source FXML file.
     * @param cssFile Source CSS file.
     */
    public GenerationTask(final File folder, final List<Infection> infections, final File fxmlFile, final File cssFile) {
        super();
        this.folder = folder;
        this.infections = infections;
        this.fxmlFile = fxmlFile;
        this.cssFile = cssFile;
    }

    @Override
    protected synchronized Void call() throws Exception {
        int exportNumber = 0;
        exportNumber = infections.stream().map((infection) -> infection.getStates().size()).reduce(exportNumber, Integer::sum);
        int totalProgress = 3 + 3 * exportNumber;
        int currentProgress = 0;
        // Load the node.
        final URL cssURL = cssFile.toURI().toURL();
        final File tempCSSFile = File.createTempFile(cssFile.getName(), null);
        try (final FileOutputStream tempCSSOutput = new FileOutputStream(tempCSSFile)) {
            Files.copy(cssFile.toPath(), tempCSSOutput);
        }
        final URL tempCSSURL = tempCSSFile.toURI().toURL();
        final URL fxmlURL = fxmlFile.toURI().toURL();
        final FXMLLoader fxmlLoader = new FXMLLoader(fxmlURL);
        final Region node = fxmlLoader.load();
//            node.getStylesheets().add(cssURL.toExternalForm());
        node.getStylesheets().add(tempCSSURL.toExternalForm());
        updateProgress(currentProgress++, totalProgress);
        if (isCancelled()) {
            return null;
        }
        //
        for (final Infection infection : infections) {
            for (final org.spc.health.epidemydesign.State state : infection.getStates()) {
                if (isCancelled()) {
                    return null;
                }
                // Export to image.
                // Apparently we can only manipulate pseudo classes on the JavaFX Application Thread.
                Platform.runLater(() -> prepareControl(node, infection, state));
                wait();
                updateProgress(currentProgress++, totalProgress);
                if (exception != null) {
                    throw exception;
                }
                if (isCancelled()) {
                    return null;
                }
                // Convert to Swing image.
                final BufferedImage swingImage = SwingFXUtils.fromFXImage(fxImage, null);
                fxImage = null;
                updateProgress(currentProgress++, totalProgress);
                // Export to file.
                final String infectionName = infection.getFileName();
                final String stateName = state.getName();
                final String outputPath = String.format("%s_%s.%s", infectionName, stateName, format); // NOI18N.
                final File outputFile = new File(folder, outputPath);
                ImageIO.write(swingImage, format, outputFile);
                updateProgress(currentProgress++, totalProgress);
            }
        }
        pseudoClassMap.clear();
        updateProgress(currentProgress++, totalProgress);
        return null;
    }

    private final HashMap<org.spc.health.epidemydesign.State, PseudoClass> pseudoClassMap = new HashMap();
    private Image fxImage;
    private Exception exception;

    private synchronized void prepareControl(final Region node, final Infection infection, final org.spc.health.epidemydesign.State state) {
        try {
            if (!isCancelled()) {
                // Change the label.
                final Label label = (Label) node.lookup(".label"); // NOI18N.
                if (label != null) {
                    label.setText(infection.getName());
                }
                // Sets the pseudo class.
                PseudoClass pseudoClass = pseudoClassMap.get(state);
                if (pseudoClass == null) {
                    pseudoClass = PseudoClass.getPseudoClass(state.getName());
                    pseudoClassMap.put(state, pseudoClass);
                }
                node.pseudoClassStateChanged(pseudoClass, true);
                final StackPane parent = new StackPane();
                parent.setStyle("-fx-background-color: transparent;"); // NOI18N.
                parent.getChildren().add(node);
                final Scene scene = new Scene(parent);
                scene.setFill(Color.TRANSPARENT);
                fxImage = scene.snapshot(null);
                // Clear scene content.
                parent.getChildren().remove(node);
                // Unsets the pseudo class.
                node.pseudoClassStateChanged(pseudoClass, false);
            }
        } catch (Exception ex) {
            exception = ex;
        } finally {
            notify();
        }
    }
}
