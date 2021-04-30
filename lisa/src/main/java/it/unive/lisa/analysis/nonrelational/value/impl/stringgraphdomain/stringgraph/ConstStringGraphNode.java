package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.List;

public class ConstStringGraphNode<C extends StringGraphNode<?,C,?, ConstStringGraphNode<C,P>>, P extends StringGraphNode<?,P, ConstStringGraphNode<C,P>,?>>
        extends StringGraphNode<ConstValues, ConstStringGraphNode<C,P>,C,P> {

    public ConstStringGraphNode() {
        super();
    }

    public ConstStringGraphNode(ConstValues constValues) {
        this();
        this.value = constValues;
    }

    @Override
    public void addForwardChild(C child) {
        throw new UnsupportedOperationException("Cannot add forward children to " + this.getClass().getName());
    }

    @Override
    public void addBackwardChild(C child) {
        throw new UnsupportedOperationException("Cannot add backward children to " + this.getClass().getName());
    }

    @Override
    public void removeChild(C child) {
        throw new UnsupportedOperationException("Cannot remove child from " + this.getClass().getName());
    }

    @Override
    public List<String> getDenotation() {
        if (this.value == null) { return new ArrayList<>(); }
        return List.of(this.value.name());
    }

    @Override
    public String toString() {
        return this.value != null ? this.value.name() : null ;
    }
}
