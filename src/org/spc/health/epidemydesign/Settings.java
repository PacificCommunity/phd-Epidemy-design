/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Allows to access user preferences.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
final class Settings {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Settings.class);
    private static final Properties VERSION_INFO = new Properties();

    static {
        final URL versionURL = Settings.class.getResource("version.properties"); // NOI18N.
        try (final InputStream input = versionURL.openStream()) {
            VERSION_INFO.load(input);
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public static Preferences getPrefs() {
        return PREFS;
    }

    public static String getVersion() {
        return String.format("%s.%s.%s", VERSION_INFO.getProperty("version.major"), VERSION_INFO.getProperty("version.minor"), VERSION_INFO.getProperty("version.release"));
    }
}
