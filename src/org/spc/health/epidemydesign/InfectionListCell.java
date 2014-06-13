/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import javafx.scene.control.ListCell;

/**
 * Displays infections in list and combo box.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class InfectionListCell extends ListCell<Infection> {

    @Override
    protected void updateItem(Infection infection, boolean empty) {
        super.updateItem(infection, empty);
        final String text = infection == null ? I18N.getString("DEFAULT_VALUE_LABEL") : infection.getName(); // NOI18N.
        setText(text);
    }
}
