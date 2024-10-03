/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign;

import javafx.application.Application;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Base class for all application controllers.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public abstract class ControllerBase {
    /**
    * The parent application of this controller.
    */
    private final ObjectProperty<Application> application = new SimpleObjectProperty<>(this, "application"); // NOI18N.

    public void setApplication(Application value) {
        application.set(value);
    }

    public Application getApplication() {
        return application.get();
    }

    public ObjectProperty<Application> applicationProperty() {
        return application;
    }
}
