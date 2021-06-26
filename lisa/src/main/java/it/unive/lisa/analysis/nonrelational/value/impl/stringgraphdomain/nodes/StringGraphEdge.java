package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes;

import java.io.Serializable;
import java.util.Objects;

public class StringGraphEdge implements Serializable {
    public enum EdgeTypes {
        FORWARD,
        BACKWARD
    }

    private final StringGraphNode<?> node;
    private final EdgeTypes type;

    public StringGraphEdge() {
        this.node = null;
        this.type = null;
    }

    public StringGraphEdge(StringGraphNode<?> node, EdgeTypes type) {
        this.node = node;
        this.type = type;
    }

    public EdgeTypes getType() {
        return type;
    }

    public StringGraphNode<?> getNode() {
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StringGraphEdge)) return false;
        StringGraphEdge that = (StringGraphEdge) o;
        return Objects.equals(node, that.node) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, type);
    }
}
