/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.util.LinkedList;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Infections.
 * <br/>User can add new ones, cannot be an enum.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class Infection {

    String name;
    final ObservableList<State> states = FXCollections.observableList(new LinkedList<>());

    @Override
    public String toString() {
        return name;
    }
}
