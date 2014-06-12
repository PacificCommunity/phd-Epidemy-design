/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import javafx.scene.paint.Color;

/**
 * State for infections.
 * <br/>User can add new ones, cannot be an enum.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class State {

    String name;
    Color color;

    @Override
    public String toString() {
        return name;
    }
}
