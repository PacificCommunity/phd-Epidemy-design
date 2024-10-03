/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
module epidemy.design {
    requires jdk.jsobject;
    requires java.desktop;
    requires java.logging;
    requires java.prefs;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;
    requires javafx.web;
    exports org.spc.health.epidemydesign;
    opens org.spc.health.epidemydesign to javafx.fxml;
    opens org.spc.health.epidemydesign.control.stateeditor to javafx.fxml;
    opens org.spc.health.epidemydesign.control.infectioneditor to javafx.fxml;
    opens org.spc.health.epidemydesign.control.generatepane to javafx.fxml;
    opens org.spc.health.epidemydesign.control.codeeditor to javafx.web;
}