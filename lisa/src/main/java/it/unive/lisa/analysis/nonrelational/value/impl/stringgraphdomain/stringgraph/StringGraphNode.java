package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConstStringGraphNode.ConstValues;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public abstract class StringGraphNode<V> implements Serializable {

	protected V value;
	public final String id;
	protected StringGraphNode<?> forwardParent;
	protected final List<StringGraphNode<?>> forwardNodes;
	protected final List<StringGraphNode<?>> backwardNodes;
	protected final List<StringGraphNode<?>> backwardParents;
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
     * @param value parameter included in the created {@link StringGraphNode}
     * @return a {@link SimpleStringGraphNode} or a ConcatNode {@link ConcatStringGraphNode}
     */
    public static StringGraphNode<?> create(String value) {
		StringGraphNode<?> result;

		value = SGNUtils.unquote(value);

		if (value.length() == 0) {
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

	/**
	 * Computes the number of nodes in a graph between two given nodes, following a depth visit.
	 * If the node given as parameter is not an ancestor of this, then returns null
	 *
	 * @param ancestor the ancestor of this from which to compute the distance (depth)
	 * @return the distance between ancestor and this if there is a forward path between them, null otherwise
	 */
	public Integer getDistance(StringGraphNode<?> ancestor) {
		Integer depth = 0;
		StringGraphNode<?> parent = getForwardParent();
		while (parent != null) {
			if (parent.equals(ancestor)) {
				break;
			}
			parent = parent.getForwardParent();
			depth++;
		}

		if (parent == null) {
			return null;
		}
		return depth;
	}

	/* Since each node can have at most one forward parent, a node can be uniquely identified by its value and its children */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StringGraphNode)) return false;
		StringGraphNode<?> that = (StringGraphNode<?>) o;
		return Objects.equals(value, that.value) && Objects.equals(forwardNodes, that.forwardNodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, forwardNodes);
	}

	@Override
	public String toString() {
		return "digraph string_graph_node_" + this.id + " { " + this.toStringAux() + " }";
	}

	public String toStringAux() {
		StringBuilder result = new StringBuilder("");
		result.append(this.id).append(" [label=\"").append(this.getLabel()).append("\"]").append(" ");
		for (StringGraphNode<?> child : this.getForwardNodes()) {
			result.append(child.toStringAux());
			result.append(this.id).append(" -> ").append(child.id).append(" ");
		}
		for (StringGraphNode<?> child : this.getBackwardNodes()) {
			result.append(this.id).append(" -> ").append(child.id).append(" ");
		}
		return result.toString();
	}
}
