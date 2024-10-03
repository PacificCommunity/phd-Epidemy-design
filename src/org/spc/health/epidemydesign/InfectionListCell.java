/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign;

import javafx.scene.control.ListCell;

import java.util.Objects;

/**
 * Displays infections in list and combo box.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class InfectionListCell extends ListCell<Infection> {

    @Override
    protected void updateItem(final Infection infection, final boolean empty) {
        super.updateItem(infection, empty);
        textProperty().unbind();
        if (!empty) {
            if (Objects.isNull(infection)) {
                final var text = I18N.getString("default-value.label"); // NOI18N.
                setText(text);
            } else {
                textProperty().bind(infection.nameProperty());
            }
        }
    }
}
