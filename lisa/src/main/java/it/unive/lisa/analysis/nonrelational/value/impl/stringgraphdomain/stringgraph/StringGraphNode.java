package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.lang.reflect.ParameterizedType;
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

	private Class<T> selfClass;
	private Class<C> childClass;
	private Class<P> parentClass;
	
	@SuppressWarnings("unchecked")
	public StringGraphNode() {
		this.selfClass = (Class<T>)
				   ((ParameterizedType)getClass().getGenericSuperclass())
				      .getActualTypeArguments()[1];
		
		this.childClass = (Class<C>)
				   ((ParameterizedType)getClass().getGenericSuperclass())
				      .getActualTypeArguments()[2];
		this.parentClass = (Class<P>)
				   ((ParameterizedType)getClass().getGenericSuperclass())
				      .getActualTypeArguments()[3];
		
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
    
    public static <T extends StringGraphNode<?,?,?,?>, V extends StringGraphNode<?,?,?,?>> 
    	Map.Entry<T,V> createEdge(T n1, V n2) {
    	return new AbstractMap.SimpleEntry<T,V>(n1,n2);
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
        child.addForwardParent(this.selfClass.cast(this));
    }

	public void addBackwardChild(C child) {
    	this.backwardNodes.add(child);
    	child.addBackwardParent(this.selfClass.cast(this));
	}

    protected void addForwardParent(P parent) {
    	this.forwardParents.add(parent);
    }

	protected void addBackwardParent(P parent) {
    	this.backwardParents.add(parent);
	}

	/**
	 * @param child child node to be removed
	 */
	public void removeChild(C child) {
    	if (this.forwardNodes.remove(child) || this.backwardNodes.remove(child)) {
    		child.removeParent(this.selfClass.cast(this));
		}
    }

    public void removeParent(P parent) {
    	this.forwardParents.remove(parent);
    	this.backwardParents.remove(parent);
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
	 *     <li>no nodes with empty denotation must be present, nodes with empty denotation should be removed</li>
	 *     <li>each OR node has strictly more than one child, otherwise it can be replaced with its child.
	 *     <li>Each child of an OR node should not be a MAX child,otherwise the entire OR subgraph
	 *     can be replaced with a MAX node</li>
	 *     <li>if a forward arc connects two OR nodes, the child node mast have an in-degree > 1,
	 *     otherwise it should be removed, assigning its children to the parent node</li>
	 *     <li>Each cycle should have at least one functor node (OR or CONCAT)
	 *     otherwise the whole cycle can be removed</li>
	 * </ul>
	 */
	public void compact() {
		// If one node denotation is empty, remove it and its whole subtree
		if (this.getDenotation().isEmpty()) {
			for(P parent: this.getForwardParents()) {
				parent.removeChild((T) this);
			}
		}
		// Call compact aux to handle subclass specific behaviours
		this.compactAux();
		for (C child : this.getForwardNodes()) {
			child.compact();
		}
	}

	/**
	 * Utility function to be override in each subclass to apply specific behaviour
	 */
	protected void compactAux() { }

	/**
	 * Says if a certain node is part of an infinite loop or not. It is a recursive methods
	 * iterating over node children which complexity is O(N), where N is the number of descendent nodes.
	 *
	 * @return true if the node is not part of a infinite loop, false otherwise
	 */
	public boolean isFinite() {
		// If the current node is a leaf, it is finite: return true
		if (this.isLeaf()) return true;
		else {
			// Otherwise the node itself and all child nodes must be checked not to have backward children
			if (this.getBackwardNodes().size() > 0) {
				return false;
			} else {
				boolean response = true;
				Iterator<C> i = this.getForwardNodes().iterator();
				while(response && i.hasNext()) {
					C node = i.next();
					response = node.isFinite();
				}
				return response;
			}
		}
	}

	/**
	 * Static method, replaces one node with another, preserving all relationships.
	 * TODO: now all casts are terrible, find a way to refactor the whole graph concept
	 *
	 * @param original the node to be replaced
	 * @param replacement the node to insert in place of original
	 */
	public static void replaceNode(StringGraphNode original, StringGraphNode replacement) {
		for (Object child : original.getForwardNodes()) {
			replacement.addForwardChild((StringGraphNode) child);
			original.removeChild((StringGraphNode) child);
		}
		for (Object child : original.getBackwardNodes()) {
			replacement.addForwardChild((StringGraphNode) child);
			original.removeChild((StringGraphNode) child);
		}
		for (Object parent : original.getForwardParents()) {
			((StringGraphNode) parent).addForwardChild((StringGraphNode) replacement);
			((StringGraphNode) parent).removeChild(original);
		}
		for (Object parent : original.getBackwardParents()) {
			((StringGraphNode) parent).addBackwardChild((StringGraphNode) replacement);
			((StringGraphNode) parent).removeChild(original);
		}
	}
	
	public List<StringGraphNode<?,?,?,?>> getPrincipalNodes() {
		return List.of(this);
	}

	public Class<T> getSelfClass() {
		return selfClass;
	}

	public Class<C> getChildClass() {
		return childClass;
	}

	public Class<P> getParentClass() {
		return parentClass;
	}
	
	
	
}
