package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Collection;

public class ConcatStringGraphNode extends StringGraphNode{

    public ConcatStringGraphNode() {
        this.value = 0;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    public ConcatStringGraphNode(Collection<StringGraphNode> parents, Collection<StringGraphNode> children) {
        this();
        this.parents.addAll(parents);
        this.parents.addAll(children);
        this.value = this.children.size();
    }


    @Override
    public void setChildren(Collection<StringGraphNode> children) {
        this.value = children.size();
        super.setChildren(children);
    }

    @Override
	public String toString() {
		return "Concat/"+value+" ["+ (children != null ? children.toString() : "") +"]";
	}
    
}
