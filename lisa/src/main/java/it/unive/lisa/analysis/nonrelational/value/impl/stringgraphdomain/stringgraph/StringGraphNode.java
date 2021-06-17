package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConstStringGraphNode.ConstValues;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public abstract class StringGraphNode<V> implements Serializable {

	protected V value;
	protected final List<StringGraphNode<?>> forwardNodes;
	protected final List<StringGraphNode<?>> backwardNodes;
	protected StringGraphNode<?> forwardParent;
	protected final List<StringGraphNode<?>> backwardParents;
	public final String id;
	private static int counter = 0;
	
	public StringGraphNode() {
		this.forwardNodes = new ArrayList<>();
		this.backwardNodes = new ArrayList<>();
		this.backwardParents = new ArrayList<>();
		this.forwardParent = null;
		this.id = "id_" + StringGraphNode.counter;
		StringGraphNode.counter += 1;
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
    public static StringGraphNode<?> create(String value) {
		StringGraphNode<?> result;

		if (value == null) {
			// EMPTY node if no value
			result = new ConstStringGraphNode(ConstValues.EMPTY);
		} else if (value.length() == 1) {
			// SIMPLE node if 1 char
			result = new SimpleStringGraphNode(value);
		} else {
			// CONCAT node if 2+ chars
			result = new ConcatStringGraphNode(value);
		}

		return result;
	}
    
    public static <T extends StringGraphNode<?>, V extends StringGraphNode<?>>
    	Map.Entry<T,V> createEdge(T n1, V n2) {
    	return new AbstractMap.SimpleEntry<>(n1,n2);
    }
    

    public int getOutDegree() {
        return this.forwardNodes.size() + this.backwardNodes.size();
    }

    public int getInDegree() {
    	return (isRoot() ? 0 : 1) + this.backwardParents.size();
    }

    public boolean isLeaf() {
        return this.getOutDegree() == 0;
    }

    public boolean isRoot() {
        return forwardParent == null;
    }

	public List<StringGraphNode<?>> getForwardNodes() {
		return forwardNodes;
	}

	public List<StringGraphNode<?>> getBackwardNodes() {
		return backwardNodes;
	}

	public StringGraphNode<?> getForwardParent() {
		return forwardParent;
	}

	public List<StringGraphNode<?>> getBackwardParents() {
		return backwardParents;
	}

	public <C extends StringGraphNode<?>> void addForwardChild(C child) {
    	this.forwardNodes.add(child);
        child.setForwardParent(this);
    }

	public <C extends StringGraphNode<?>> void addForwardChild(int index, C child) {
		this.forwardNodes.add(index, child);
		child.setForwardParent(this);
	}

	public <C extends StringGraphNode<?>> void addBackwardChild(C child) {
    	this.backwardNodes.add(child);
    	child.addBackwardParent(this);
	}

	protected <P extends StringGraphNode<?>> void setForwardParent(P forwardParent) {
		this.forwardParent = forwardParent;
	}

	protected <P extends StringGraphNode<?>> void addBackwardParent(P parent) {
    	this.backwardParents.add(parent);
	}

	public <C extends StringGraphNode<?>> void removeChild(C child) {
    	if (this.forwardNodes.remove(child) || this.backwardNodes.remove(child)) {
    		child.removeParent(this);
		}
    }

    public <P extends StringGraphNode<?>> void removeParent(P parent) {
    	if (!isRoot() && getForwardParent().equals(parent)) { setForwardParent(null); }
    	this.backwardParents.remove(parent);
    }

	public List<StringGraphNode<?>> getChildren() {
		return Stream
				.concat(getForwardNodes().stream(), getBackwardNodes().stream())
				.distinct()
				.collect(Collectors.toList());
	}

	public V getValue() { return value; }

	public void setValue(V value) { this.value = value; }

	public abstract String getLabel();

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
	 * Normalizes the String graph having root in the current node. In order to do so, it applies the following rules:
	 * <ul>
	 *     <li><strong>Rule 1:</strong>no nodes with empty denotation must be present, nodes with empty denotation
	 *     should be removed</li>
	 * 	   <li><strong>Rule 2:</strong>each OR node has strictly more than one child, otherwise it can be replaced
	 * 	   with its child.</li>
	 * 	   <li><strong>Rule 3</strong>Each child of an OR node should not be a MAX child,otherwise the entire OR
	 * 	   subgraph can be replaced with a MAX node</li>
	 * 	   <li><strong>Rule 4:</strong>if a forward arc connects two OR nodes, the child node mast have an
	 * 	   in-degree > 1, otherwise it should be removed, assigning its children to the parent node</li>
	 * 	   <li><strong>Rule 5:</strong>Each cycle should have at least one functor node (OR or CONCAT)
	 * 	   otherwise the whole cycle can be removed</li>
	 *     <li><strong>Rule 6:</strong> Concat nodes with only one child should be replaced with their only child</li>
	 *     <li><strong>Rule 7:</strong> Concat nodes with only MAX node children should be replaced
	 *     with a MAX node</li>
	 *     <li><strong>Rule 8:</strong> If two concat nodes are successive children of a concat parent, these two
	 *     nodes should be replaced with a single node having, as children, the children of the two replaced nodes
	 *     placed in the same order they were before (only if both the replaced nodes had only one parent)</li>
	 *     <li><strong>Rule 9:</strong> If a concat node has a concat parent, and its in-degree is 1, then it should
	 *     be replaced by its children</li>
	 * </ul>
	 * All the rules have to be applied only in forward direction
	 * <strong>NB:</strong> Rule 9 incorporates rule 8, since the action described in rule 3 will completely be
	 * overwritten by rule 4. Plus, note that rule 5 is always implicitly satisfied because simple and const nodes
	 * are not allowed to have children, so a cycle will always have at least one functor node.
	 */
	@SuppressWarnings("unchecked")
	public StringGraphNode<?> normalize() {
		StringGraphNode<V> normalized;
		try {
			normalized = this.getClass().getDeclaredConstructor().newInstance();
			if (normalized instanceof SimpleStringGraphNode || normalized instanceof ConstStringGraphNode)
				normalized.setValue(this.getValue());
		} catch (Exception e) {
			return new ConstStringGraphNode(ConstValues.EMPTY);
		}

		for (StringGraphNode<?> child : this.getForwardNodes()) {
			if (!(child.getDenotation().isEmpty())) {
				StringGraphNode<?> newChild = child.normalize();
				normalized.addForwardChild(newChild);
			}
		}

		return normalized.normalizeAux();
	}

	/**
	 * Utility function to be override in each subclass to apply specific behaviour
	 */
	protected StringGraphNode<?> normalizeAux() { return this; }

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
				Iterator<StringGraphNode<?>> i = this.getForwardNodes().iterator();
				while(response && i.hasNext()) {
					StringGraphNode<?> node = i.next();
					response = node.isFinite();
				}
				return response;
			}
		}
	}

	/**
	 * Creates the list of principal nodes following the algorithm described in Section 4.4.1:
	 * <ul>
	 *     <li><strong>Case 1:</strong> if this is an OR node, it is the union of all principal nodes of each child of this node</li>
	 *     <li><strong>Case 2:</strong> for every other type of node, it returns the node itself</li>
	 * </ul>
	 *
	 * @return the list of Principal Nodes
	 */
	public Set<StringGraphNode<?>> getPrincipalNodes() {
		return Set.of(this);
	}

	public Set<String> getPrincipalLabels() {
		return getPrincipalNodes().stream()
				.map(StringGraphNode::getLabel)
				.collect(Collectors.toSet());
	}


	public boolean isLessOrEqual(StringGraphNode<?> other) {
		return SGNUtils.partialOrderAux(this, other, new HashSet<>());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StringGraphNode<?> that = (StringGraphNode<?>) o;
		return Objects.equals(value, that.value) && Objects.equals(forwardParent, that.forwardParent) && Objects.equals(backwardParents, that.backwardParents);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}
}
