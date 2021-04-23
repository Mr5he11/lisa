package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class OrStringGraphNode extends StringGraphNode {

    public OrStringGraphNode() {
        this.value = null;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    public OrStringGraphNode(Collection<StringGraphNode> parents, Collection<StringGraphNode> children) {
        this();
        this.parents.addAll(parents);
        this.parents.addAll(children);
    }
    
    @Override
	public String toString() {
		return "OR ["+ children != null ? children.toString() : "" +"]";
	}
}
