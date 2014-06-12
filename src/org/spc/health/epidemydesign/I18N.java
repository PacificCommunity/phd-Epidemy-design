/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Allows to retrieve I18N text..
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class I18N {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("org.spc.health.epidemydesign.strings"); // NOI18N.

    public static ResourceBundle getResourceBundle() {
        return BUNDLE;
    }

    public static String getString(String key) {
        return getString(key, key);
    }

    public static String getString(final String key, final String defaultValue) {
        String result = defaultValue;
        try {
            result = BUNDLE.getString(key);
        } catch (MissingResourceException ex) {
            Logger.getLogger(I18N.class.getName()).warning(ex.getMessage());
        }
        return result;
    }
}
