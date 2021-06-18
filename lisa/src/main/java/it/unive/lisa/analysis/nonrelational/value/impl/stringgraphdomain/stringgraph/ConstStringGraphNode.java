package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.List;

public class ConstStringGraphNode extends StringGraphNode<ConstStringGraphNode.ConstValues> {
    public enum ConstValues {
        MAX,
        MIN,
        EMPTY,
        ALL_STRINGS
    }

    public ConstStringGraphNode() {
        super();
    }

    public ConstStringGraphNode(ConstValues constValues) {
        this();
        this.value = constValues;
    }

    @Override
    public <C extends StringGraphNode<?>> void addForwardChild(C child) {
        throw new UnsupportedOperationException("Cannot add forward children to " + this.getClass().getName());
    }

    @Override
    public <C extends StringGraphNode<?>> void addBackwardChild(C child) {
        throw new UnsupportedOperationException("Cannot add backward children to " + this.getClass().getName());
    }

    @Override
    public <C extends StringGraphNode<?>> void removeChild(C child) {
        throw new UnsupportedOperationException("Cannot remove child from " + this.getClass().getName());
    }

    @Override
    public List<String> getDenotation() {
        if (this.value == null  || ConstValues.MIN.equals(this.value) || ConstValues.EMPTY.equals(this.value) ) { return new ArrayList<>(); }
        return List.of(this.value.name());
    }

    /*
    @Override
    public String toString() { return this.getLabel(); }
     */

    @Override
    public String getLabel() {
        return this.value != null ? this.value.name() : null ;
    }
}
