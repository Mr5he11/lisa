package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.SimpleStringGraphNode.ConstValue;

public abstract class StringGraphNode {
    protected Object value;
    protected List<StringGraphNode> children;
    protected List<StringGraphNode> parents;
    
    /**
     * Creates a node starting from the string value parameter.
     * It checks whether the string has only one character, producing a {@link SimpleStringGraphNode} 
     * or if it has multiple characters. If so, this produces a String Graph with a {@link ConcatStringGraphNode} as root 
     * and all characters of the string as {@link SimpleStringGraphNode} nodes
     *
     * 
     * @param value parameter included in the created {@link StringGraphNode}
     * @return a {@link SimpleStringGraphNode} or a ConcatNode {@link ConcatStringGraphNode} 
     */
    public static StringGraphNode create(String value) {
    	
    	// Create EMPTY node
    	if (value == null) 
    		return new SimpleStringGraphNode(ConstValue.EMPTY);
    	
    	// Create SIMPLE node with 1 char
    	if (value.length() == 1)
    		return new SimpleStringGraphNode(value);
    	
    	
    	StringGraphNode root = new ConcatStringGraphNode();

    	// Create CONCAT node with all chars of value as children 
    	List<StringGraphNode> charNodes = Stream.of(value.split(""))	// create stream of all characters
    			.map(c -> new SimpleStringGraphNode(c))			// map them to a StringGraphNode.
    			.collect(Collectors.toList());							// collect as list
    	root.setChildren(charNodes);
    	return root;
    }
    

    public int getOutDegree() {
        return this.children.size();
    }

    public int getInDegree() {
        return this.parents.size();
    }

    public boolean isLeaf() {
        return this.getOutDegree() == 0;
    }

    public boolean isRoot() {
        return this.getInDegree() == 0;
    }

    public void setChildren(Collection<StringGraphNode> children) {
        this.children = new ArrayList<>(children);
    }

    public void setParents(Collection<StringGraphNode> parents) {
        this.parents = new ArrayList<>(parents);
    }

    public List<StringGraphNode> getChildren() {
        return this.children;
    }

    public List<StringGraphNode> getParents() {
        return this.parents;
    }

    public void addChild(StringGraphNode child) {
        this.children.add(child);
    }

    public void addParent(StringGraphNode parent) {
        this.parents.add(parent);
    }

    public void removeChild(StringGraphNode child) {
        this.children.remove(child);
    }

    public void removeParent(StringGraphNode parent) {
        this.parents.remove(parent);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((children == null) ? 0 : children.hashCode());
		result = prime * result + ((parents == null) ? 0 : parents.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringGraphNode other = (StringGraphNode) obj;
		if (children == null) {
			if (other.children != null)
				return false;
		} else if (!children.equals(other.children))
			return false;
		if (parents == null) {
			if (other.parents != null)
				return false;
		} else if (!parents.equals(other.parents))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
    
    
    
}
