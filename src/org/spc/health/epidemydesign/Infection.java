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
public final class Infection {

    private final String name;
    private final ObservableList<State> states = FXCollections.observableList(new LinkedList<>());

    public Infection(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ObservableList<State> getStates() {
        return states;
    }

    @Override
    public String toString() {
        return name;
    }
}
