package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class StringGraphNode<T> {
    protected T value;
	protected final List<StringGraphNode> forwardNodes;
	protected final List<StringGraphNode> backwardNodes;
	protected final List<StringGraphNode> forwardParents;
	protected final List<StringGraphNode> backwardParents;

	public StringGraphNode() {
		this.forwardNodes = new ArrayList<>();
		this.backwardNodes = new ArrayList<>();
		this.forwardParents = new ArrayList<>();
		this.backwardParents = new ArrayList<>();
	}

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
        return this.forwardNodes.size() + this.backwardNodes.size();
    }

    public int getInDegree() { return this.forwardParents.size() + this.backwardParents.size(); }

    public boolean isLeaf() {
        return this.getOutDegree() == 0;
    }

    public boolean isRoot() {
        return this.forwardParents.isEmpty();
    }

	public List<StringGraphNode> getForwardNodes() {
		return forwardNodes;
	}

	public List<StringGraphNode> getBackwardNodes() {
		return backwardNodes;
	}

	public List<StringGraphNode> getForwardParents() {
		return forwardParents;
	}

	public List<StringGraphNode> getBackwardParents() {
		return backwardParents;
	}

	protected void addForwardChild(StringGraphNode child) {
    	this.forwardNodes.add(child);
        child.addForwardParent(this);
    }

	protected void addBackwardChild(StringGraphNode child) {
    	this.backwardNodes.add(child);
    	child.addBackwardParent(this);
	}

    private void addForwardParent(StringGraphNode parent) {
    	this.forwardParents.add(parent);
    }

	private void addBackwardParent(StringGraphNode parent) {
    	this.backwardParents.add(parent);
	}

	public List<StringGraphNode> getChildren() {
    	return Stream.concat(getForwardNodes().stream(), getBackwardNodes().stream()).distinct().collect(Collectors.toList());
	}

	/**
	 * CANNOT REMOVE BACKWARDS CHILDREN!
	 *
	 * @param child child node to be removed
	 */
    protected void removeChild(StringGraphNode child) {
    	if (this.forwardNodes.remove(child) /*|| this.backwardNodes.remove(child)*/) {
    		child.removeParent(this); /* Luke, I am NOT you father */
		}
    }

    private void removeParent(StringGraphNode parent) {
    	this.forwardParents.remove(parent);
    	/*this.backwardParents.remove(parent);*/
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
	public boolean isFinite(StringGraphNode root) {
		for (Object el : this.getChildren()) {
			StringGraphNode child = (StringGraphNode) el;
			if (root == this || !child.isFinite(root)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StringGraphNode<?> that = (StringGraphNode<?>) o;
		return Objects.equals(value, that.value) && Objects.equals(forwardNodes, that.forwardNodes) && Objects.equals(backwardNodes, that.backwardNodes) && Objects.equals(forwardParents, that.forwardParents) && Objects.equals(backwardParents, that.backwardParents);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, forwardNodes, backwardNodes, forwardParents, backwardParents);
	}
}
