package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.Collection;
import java.util.HashSet;

public class OrStringGraphNode extends StringGraphNode {

    public OrStringGraphNode() {
        this.value = null;
        this.children = new HashSet<>();
        this.parents = new HashSet<>();
    }

    public OrStringGraphNode(Collection<StringGraphNode> parents, Collection<StringGraphNode> children) {
        this();
        this.parents.addAll(parents);
        this.parents.addAll(children);
    }
}
