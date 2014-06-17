/***********************************************************************
 *  Copyright - Secretariat of the Pacific Community                   *
 *  Droit de copie - Secrétariat Général de la Communauté du Pacifique *
 *  http://www.spc.int/                                                *
 ***********************************************************************/
package org.spc.health.epidemydesign;

import java.util.LinkedList;
import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Infections.
 * <br/>User can add new ones, cannot be an enum.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class Infection implements Comparable<Infection> {

    private final String id;

    /**
     * Creates a new instance.
     * @param name The name of the infection.
     * @param fileName The filename.
     * @throws IllegalArgumentException If {@code} name was {@code null}.
     */
    public Infection(final String name, final String fileName) throws IllegalArgumentException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty.");
        }
        id = name;
        setName(name);
        setFileName((fileName == null || fileName.trim().isEmpty()) ? name.replaceAll("\\?", "U").replaceAll("\\s", "") : fileName); // NOI18N.
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Infection) ? id.equals(((Infection) obj).id) : false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int compareTo(Infection otherInfection) {
        return id.compareTo(otherInfection.id);
    }
    ////////////////////////////////////////////////////////////////////////////
    private final StringProperty name = new SimpleStringProperty(this, "name"); // NOI18N.

    public final String getName() {
        return name.get();
    }

    public final void setName(String value) {
        name.set(value);
    }

    public final StringProperty nameProperty() {
        return name;
    }

    private final StringProperty fileName = new SimpleStringProperty(this, "fileName"); // NOI18N.

    public String getFileName() {
        return fileName.get();
    }

    public final void setFileName(final String value) {
        fileName.set(value);
    }

    public final StringProperty fileNameProperty() {
        return fileName;
    }

    private final ObservableList<State> states = FXCollections.observableList(new LinkedList<>());

    public ObservableList<State> getStates() {
        return states;
    }
}
