package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class StringGraphNode<T> {
    protected T value;
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
    		return SimpleStringGraphNode.EMPTY;
    	
    	// Create SIMPLE node with 1 char
    	if (value.length() == 1)
    		return new SimpleStringGraphNode(value);
    	
    	
    	StringGraphNode root = new ConcatStringGraphNode();

    	// Create CONCAT node with all chars of value as children 
    	List<StringGraphNode> charNodes = Stream.of(value.split(""))	// create stream of all characters
    			.map(SimpleStringGraphNode::new)					// map them to a StringGraphNode.
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

	public T getValue() { return value; }

	public void setValue(T value) { this.value = value; }

	/**
	 * Produces the denotation of the string graph having root in the current object.
	 * The denotation of a string graph is the representation of all possible finite strings that can be represented
	 * through the string graph. In order to obtain this result,
	 * the definition 4.1 of <a href="https://onlinelibrary.wiley.com/doi/abs/10.1002/spe.2218">this paper</a>
	 * has been applied.
	 *
	 * @return the denotation of the string graph, as a List of Strings.
	 */
	public abstract List<String> getDenotation();

	/**
	 * Compacts the given string graph, modifying current String graph instance.
	 * Compaction rules are the following:
	 * <ul>
	 *     <li>no nodes with empty denotation must be present, nodes with empty declaration should be removed</li>
	 *     <li>each OR node has strictly more than one child and each child should not be a MAX child,
	 *     otherwise the entire OR subgraph can be replaced with a MAX node</li>
	 *     <li>if a forward arc connects two OR nodes, the child node mast have an in-degree > 1,
	 *     otherwise it should be removed, assigning its children to the parent node</li>
	 *     <li>Each cycle should have at least one functor node (OR or CONCAT)
	 *     otherwise the whole cycle can be removed</li>
	 * </ul>
	 */
	public void compact() {
	}

	/**
	 * Says if a certain node is part of an infinite loop or not. It is a recursive methods
	 * iterating over node children which complexity is O(N), where N is the number of descendent nodes.
	 *
	 * @return true if the node is not part of a infinite loop, false otherwise
	 */
	public boolean isFinite() {
		for (Object el : this.getChildren()) {
			StringGraphNode child = (StringGraphNode) el;
			if (child == this || !child.isFinite()) {
				return false;
			}
		}
		return true;
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
			return other.value == null;
		} else return value.equals(other.value);
	}
    
    
    
}
