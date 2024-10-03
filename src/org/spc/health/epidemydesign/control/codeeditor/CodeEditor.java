/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign.control.codeeditor;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CSS and XML code editor with syntax highlight.
 * <br/>We use HTML5 for this editor.
 * <br/>The underlying JavaScript editor is using <a href="http://codemirror.net/">CodeMirror</a>.
 *
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public class CodeEditor extends Region {

    private static final Logger LOGGER = Logger.getLogger(CodeEditor.class.getName());
    private static int ID_GENERATOR = 0;
    private final int id = ID_GENERATOR++;
    /**
     * The delegated web view.
     */
    private final WebView webView = new WebView();
    /**
     * Indicates whether this control has been initialized.
     */
    private final ReadOnlyBooleanWrapper initialized = new ReadOnlyBooleanWrapper(this, "initialized", false);
    /**
     * Current mode of the editor.
     */
    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(this, "mode"); // NOI18N.
    /**
     * Called whenever the mode is invalidated.
     */
    private final InvalidationListener modeInvalidationListener = (final Observable observable) -> Platform.runLater(this::switchPeerMode);
    /**
     * Current content of the editor.
     */
    private final StringProperty text = new SimpleStringProperty(this, "text"); // NOI18N.
    /**
     * Action to execute once the editor has been initialized.
     */
    private final ObjectProperty<EventHandler<ActionEvent>> onInitialized = new SimpleObjectProperty<>(this, "onInitialized"); // NOI18N.
    /**
     * Called whenever the initialized property changes value.
     */
    private final ChangeListener<Boolean> initializedChangeListener = (_, _, newValue) -> {
        if (newValue) {
            Optional.ofNullable(getOnInitialized())
                    .ifPresent(eventHandler -> {
                        try {
                            ActionEvent actionEvent = new ActionEvent(CodeEditor.this, null);
                            eventHandler.handle(actionEvent);
                        } catch (Throwable ex) {
                            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                        }
                    });
        }
    };
    /**
     * Trying to prevent early GC.
     */
    private final Bridge bridge = new Bridge();
    ////////////////////////////////////////////////////////////////////////////
    private boolean isEditing = false;
    /**
     * Called whenever the text is invalidated.
     */
    private final InvalidationListener textInvalidationListener = _ -> {
        if (!isEditing) {
            Platform.runLater(() -> {
                clearPeerContent();
                pushTextToPeer();
            });
        }
    };
    /**
     * Called when the state of the web engine loader changes.
     */
    private final ChangeListener<Worker.State> peerLoadStateChangeListener = (_, oldValue, newValue) -> {
        LOGGER.log(Level.INFO, "%d: %s -> %s%n".formatted(id, oldValue, newValue));
        switch (newValue) {
            case SUCCEEDED -> {
                LOGGER.log(Level.INFO, "Code editor peer load succeeded.");
                // Installing bridge object.
                final var jsObject = (JSObject) webView.getEngine().executeScript("window"); // NOI18N.
                jsObject.setMember("java", bridge); // NOI18N.
                // Add property listeners.
                modeProperty().addListener(modeInvalidationListener);
                textProperty().addListener(textInvalidationListener);
                // Finishing initialization.
                initialized.set(true);
            }
            case CANCELLED -> LOGGER.log(Level.INFO, "Code editor peer load canceled.");
            case FAILED -> {
                final Throwable ex = webView.getEngine().getLoadWorker().getException();
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    };

    /**
     * Creates a new instance.
     */
    public CodeEditor() {
        super();
        setId("codeEditor"); // NOI18N.
        getStyleClass().add("code-editor"); // NOI18N.
        getChildren().add(webView);
        //
        initialized.addListener(initializedChangeListener);
        Platform.runLater(this::initializePeer);
    }

    /**
     * Initialize the peer control.
     */
    private void initializePeer() {
        webView.getEngine().getLoadWorker().stateProperty().addListener(peerLoadStateChangeListener);
        Optional.ofNullable(getClass().getResource("CodeEditor.html")) // NOI18N.
                .map(URL::toExternalForm)
                .ifPresent(webView.getEngine()::load);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    ////////////////////////////////////////////////////////////////////////////

    public final ReadOnlyBooleanProperty initializedProperty() {
        return initialized.getReadOnlyProperty();
    }

    ////////////////////////////////////////////////////////////////////////////

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        final double width = getWidth();
        final double height = getHeight();
        final Insets insets = getInsets();
        webView.resizeRelocate(insets.getLeft(), insets.getTop(), width - (insets.getLeft() + insets.getRight()), height - (insets.getTop() + insets.getBottom()));
    }

    /**
     * Changes the editor mode of the HTML5 peer.
     */
    private void switchPeerMode() {
        final Optional<Mode> mode = Optional.ofNullable(getMode());
        mode.ifPresent((final Mode m) -> {
            final String command = String.format("setMode('%s');", m.toString().toLowerCase()); // NOI18N.
            webView.getEngine().executeScript(command);
        });
    }

    /**
     * Clear the content of the HTML5 peer.
     */
    private void clearPeerContent() {
        try {
            isEditing = true;
            webView.getEngine().executeScript("clearText()"); // NOI18N.
        } finally {
            isEditing = false;
        }
    }

    /**
     * Push text from this control to our HTML5 peer.
     */
    private void pushTextToPeer() {
        Optional.ofNullable(getText())
                .ifPresent(text -> {
                    try {
                        isEditing = true;
                        final String content = text
                                .replaceAll("\\\\", "\\\\\\\\") // NOI18N.
                                .replaceAll("\n", "\\\\n") // NOI18N.
                                .replaceAll("'", "\\\\'"); // NOI18N.
                        final String command = String.format("setText('%s');", content); // NOI18N.
                        webView.getEngine().executeScript(command);
                    } finally {
                        isEditing = false;
                    }
                });
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Pull text from our HTML5 peer to this control.
     */
    private void pullTextFromPeer() {
        if (isEditing) {
            return;
        }
        try {
            isEditing = true;
            final String result = (String) webView.getEngine().executeScript("getText()"); // NOI18N.
            setText(result);
        } finally {
            isEditing = false;
        }
    }

    public final Mode getMode() {
        return mode.get();
    }

    public final void setMode(final Mode value) {
        mode.set(value);
    }

    public final ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    public final String getText() {
        return text.get();
    }

    public final void setText(final String value) {
        text.set(value);
    }

    public final StringProperty textProperty() {
        return text;
    }

    public final EventHandler<ActionEvent> getOnInitialized() {
        return onInitialized.get();
    }

    public final void setOnInitialized(final EventHandler<ActionEvent> value) {
        onInitialized.set(value);
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onInitializedProperty() {
        return onInitialized;
    }

    /**
     * Code modes in this editor.
     *
     * @author Fabrice Bouyé (fabriceb@spc.int)
     */
    public enum Mode {

        XML, CSS
    }

    /**
     * The bridge class that is provided to JavaScript for bidirectionnal dialog.
     * <br/>Had to be public to avoid some exceptions from being thrown.
     *
     * @author Fabrice Bouyé (fabriceb@spc.int)
     */
    public final class Bridge {

        public void updateText() {
            LOGGER.entering(Bridge.class.getName(), "updateText");
            pullTextFromPeer();
            LOGGER.exiting(Bridge.class.getName(), "updateText");
        }
    }
}
