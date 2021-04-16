package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.Collection;
import java.util.HashSet;

public class SimpleStringGraphNode extends StringGraphNode{

    enum ConstValue {
        MAX,
        MIN,
        EMPTY
    }

    public SimpleStringGraphNode() {
        this.value = ConstValue.EMPTY;
        this.children = new HashSet<>();
        this.parents = new HashSet<>();
    }

    public SimpleStringGraphNode(String value) {
        this();
        if (value.length() != 1) throw new IllegalArgumentException("Value of SimpleStringGraphNode must be of length 1");
        this.value = value;
    }

    public SimpleStringGraphNode(String value, Collection<StringGraphNode> parents) {
        this(value);
        this.parents.addAll(parents);
    }

}
