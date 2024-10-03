/*
 Copyright - Pacific Community
 Droit de copie - Communauté du Pacifique
 http://www.spc.int/
*/
package org.spc.health.epidemydesign;

import java.util.Objects;
import javafx.scene.paint.Color;

/**
 * State for infections.
 * <br/>User can add new ones, cannot be an enum.
 * @author Fabrice Bouyé (fabriceb@spc.int)
 */
public final class State implements Comparable<State> {

    private final String id;
    private final String name;
    private final Color color;

    public State(final String name, final Color color) {
        this.id = name;
        this.name = name;
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof State state && id.equals(state.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public int compareTo(State otherState) {
        return id.compareTo(otherState.id);
    }

    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return name;
    }
}
