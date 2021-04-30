package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @param <V> type of Value
 * @param <T> type of current node
 * @param <C> type of children nodes of T: it has current node type C and parent node type T
 * @param <P> type of parent nodes of T: it has current node type P and children node type T
 */
public abstract class StringGraphNode
		<V,
		T extends StringGraphNode<V, T, C, P>,
		C extends StringGraphNode<?,C,?,T>,
		P extends StringGraphNode<?,P,T,?>> {

	protected V value;
	protected final List<C> forwardNodes;
	protected final List<C> backwardNodes;
	protected final List<P> forwardParents;
	protected final List<P> backwardParents;

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
    public static StringGraphNode<?,?,?,?> create(String value) {
		StringGraphNode<?,?,?,?> result;

		if (value == null) {
			// EMPTY node if no value
			result = new ConstStringGraphNode<>(ConstValues.EMPTY);
		} else if (value.length() == 1) {
			// SIMPLE node if 1 char
			result = new SimpleStringGraphNode<>(value);
		} else {
			// CONCAT node if 2+ chars
			result = new ConcatStringGraphNode<>(value);
		}

		return result;
	}
    

    public int getOutDegree() {
        return this.forwardNodes.size() + this.backwardNodes.size();
    }

    public int getInDegree() {
    	return this.forwardParents.size() + this.backwardParents.size();
    }

    public boolean isLeaf() {
        return this.getOutDegree() == 0;
    }

    public boolean isRoot() {
        return this.forwardParents.isEmpty();
    }

	public List<C> getForwardNodes() {
		return forwardNodes;
	}

	public List<C> getBackwardNodes() {
		return backwardNodes;
	}

	public List<P> getForwardParents() {
		return forwardParents;
	}

	public List<P> getBackwardParents() {
		return backwardParents;
	}

	public void addForwardChild(C child) {
    	this.forwardNodes.add(child);
        child.addForwardParent((T)this);
    }

	public void addBackwardChild(C child) {
    	this.backwardNodes.add(child);
    	child.addBackwardParent((T)this);
	}

    protected void addForwardParent(P parent) {
    	this.forwardParents.add(parent);
    }

	protected void addBackwardParent(P parent) {
    	this.backwardParents.add(parent);
	}

	/**
	 * CANNOT REMOVE BACKWARDS CHILDREN!
	 *
	 * @param child child node to be removed
	 */
	public void removeChild(C child) {
    	if (this.forwardNodes.remove(child) /*|| this.backwardNodes.remove(child)*/) {
    		child.removeParent((T)this); /* Luke, I am NOT you father */
		}
    }

    public void removeParent(P parent) {
    	this.forwardParents.remove(parent);
    	/*this.backwardParents.remove(parent);*/
    }

	public List<C> getChildren() {
		return Stream.concat(getForwardNodes().stream(), getBackwardNodes().stream()).distinct().collect(Collectors.toList());
	}

	public V getValue() { return value; }

	public void setValue(V value) { this.value = value; }

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
	public boolean isFinite(StringGraphNode<?,?,?,?> root) {
		for (C child : this.getChildren()) {
			if (root == this || !child.isFinite(root)) {
				return false;
			}
		}
		return true;
	}
}
