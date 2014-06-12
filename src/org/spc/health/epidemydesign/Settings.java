/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.util.prefs.Preferences;

/**
 * Allows to access user preferences.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);

    public static Preferences getPrefs() {
        return PREFS;
    }
}
