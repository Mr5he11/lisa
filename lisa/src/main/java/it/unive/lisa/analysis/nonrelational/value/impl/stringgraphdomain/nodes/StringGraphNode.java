package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.SGNUtils;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.ConstStringGraphNode.ConstValues;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


public abstract class StringGraphNode<V> implements Serializable {

	protected V value;
	public final String id;
	protected StringGraphNode<?> forwardParent;
	protected final List<StringGraphEdge> childrenEdges;
	protected final List<StringGraphNode<?>> backwardParents;
	private static int counter = 0;
	private final Set<StringGraphNode<?>> is;
	
	public StringGraphNode() {
		this.is = new HashSet<>();
		this.forwardParent = null;
		this.childrenEdges = new ArrayList<>();
		this.backwardParents = new ArrayList<>();
		this.id = "id_" + StringGraphNode.counter;
		StringGraphNode.counter += 1;
	}
    
    public static <T extends StringGraphNode<?>, V extends StringGraphNode<?>>
    	Map.Entry<T,V> createEdge(T n1, V n2) {
    	return new AbstractMap.SimpleEntry<>(n1,n2);
    }

    public int getOutDegree() {
        return this.childrenEdges.size();
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

	public List<StringGraphEdge> getChildrenEdges() {
		return childrenEdges;
	}

	public List<StringGraphNode<?>> getChildren() {
		return childrenEdges.stream().map(StringGraphEdge::getNode).collect(Collectors.toList());
	}

	public List<StringGraphNode<?>> getForwardChildren() {
		return childrenEdges
				.stream()
				.filter(c -> c.getType() == StringGraphEdge.EdgeTypes.FORWARD)
				.map(StringGraphEdge::getNode)
				.collect(Collectors.toList());
	}

	public List<StringGraphNode<?>> getBackwardChildren() {
		return childrenEdges
				.stream()
				.filter(c -> c.getType() == StringGraphEdge.EdgeTypes.BACKWARD)
				.map(StringGraphEdge::getNode)
				.collect(Collectors.toList());
	}

	public StringGraphNode<?> getForwardParent() {
		return forwardParent;
	}

	public List<StringGraphNode<?>> getBackwardParents() {
		return backwardParents;
	}

	private <C extends StringGraphNode<?>> void addChild(C child, StringGraphEdge.EdgeTypes type) {
		this.childrenEdges.add(new StringGraphEdge(child, type));
	}

	private <C extends StringGraphNode<?>> void addChild(C child, StringGraphEdge.EdgeTypes type, int index) {
		this.childrenEdges.add(index, new StringGraphEdge(child, type));
	}

	public <C extends StringGraphNode<?>> void addForwardChild(C child) {
		if (!child.isRoot()) { child.getForwardParent().removeChild(child); }
		addChild(child, StringGraphEdge.EdgeTypes.FORWARD);
		child.setForwardParent(this);
    }

	public <C extends StringGraphNode<?>> void addForwardChild(int index, C child) {
		if (!child.isRoot()) { child.getForwardParent().removeChild(child); }
		addChild(child, StringGraphEdge.EdgeTypes.FORWARD, index);
		child.setForwardParent(this);
	}

	public <C extends StringGraphNode<?>> void addBackwardChild(C child) {
		addChild(child, StringGraphEdge.EdgeTypes.BACKWARD);
		child.addBackwardParent(this);
	}

	public <C extends StringGraphNode<?>> void addBackwardChild(int index, C child) {
		addChild(child, StringGraphEdge.EdgeTypes.BACKWARD, index);
		child.addBackwardParent(this);
	}

	protected <P extends StringGraphNode<?>> void setForwardParent(P forwardParent) {
    	this.forwardParent = forwardParent;
	}

	protected <P extends StringGraphNode<?>> void addBackwardParent(P parent) {
    	this.backwardParents.add(parent);
	}

	public <C extends StringGraphNode<?>> void removeChild(C child) {
    	if (this.childrenEdges.removeIf(e -> e.getNode().equals(child)))
			child.removeParent(this);
    }

	public <P extends StringGraphNode<?>> void removeParent(P parent) {
		if (parent.equals(this.forwardParent))
			this.forwardParent = null;
		this.backwardParents.remove(parent);
	}

	public V getValue() { return value; }

	public void setValue(V value) { this.value = value; }

	public abstract String getLabel();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StringGraphNode)) return false;
		StringGraphNode<?> that = (StringGraphNode<?>) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	/**
	 * Generates the dot representation of the string graph rooted in this.
	 *
	 * @return the dot representation of this
	 */
	@Override
	public String toString() {
		return "digraph string_graph_node_" + this.id + " { " + String.join("", this.toStringAux()) + " }";
		//return getLabel();
	}

	public Set<String> toStringAux() {
		Set<String> result = new LinkedHashSet<>();
		result.add(this.id + " [label=\"" + this.getLabel() + "\"]\n"); // TODO getLabel()
		for (StringGraphNode<?> child : this.getForwardChildren()) {
			result.addAll(child.toStringAux());
			result.add(this.id+ " -> " + child.id + "\n");
		}
		for (StringGraphNode<?> child : this.getBackwardChildren()) {
			result.add(this.id + " -> " + child.id + " [style=dashed]\n");
		}
		return result;
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
		value = SGNUtils.unquote(value);
		if (value.length() == 0)
			return new ConstStringGraphNode(ConstValues.EMPTY);
		if (value.length() == 1)
			return new SimpleStringGraphNode(value);
		return new ConcatStringGraphNode(value);
	}

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
			if (this.getBackwardChildren().size() > 0) {
				return false;
			} else {
				boolean response = true;
				Iterator<StringGraphNode<?>> i = this.getForwardChildren().iterator();
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

	/**
	 * Generates the set of principal labels of this, starting from its principal nodes.
	 *
	 * @return a set of strings representing the principal labels of this
	 */
	public Set<String> getPrincipalLabels() {
		return getPrincipalNodes().stream()
				.map(StringGraphNode::getLabel)
				.collect(Collectors.toSet());
	}

	/**
	 * Computes if this is less or equal then other, more in-depth documentation can be found at {@link SGNUtils}.
	 *
	 * @param other the node this should be compared with
	 * @return true if this is less or equal to other, false otherwise
	 */
	public boolean isLessOrEqual(StringGraphNode<?> other) {
		return SGNUtils.partialOrderAux(this, other, new HashSet<>());
	}

	/**
	 * Computes the depth of a node, starting from its root
	 *
	 * @return the depth of the node
	 */
	public int getDepth() {
		StringGraphNode<?> n = this;
		int depth = 0;
		while(!n.isRoot()) {
			depth += 1;
			n = n.getForwardParent();
		}
		return depth;
	}

	/**
	 * Computes the chain of ancestors of this
	 *
	 * @return a list of all ancestors of this
	 */
	public List<StringGraphNode<?>> getAncestors() {
		StringGraphNode<?> root = this;
		List<StringGraphNode<?>> ancestors = new ArrayList<>();
		while(!root.isRoot()) {
			root = root.getForwardParent();
			ancestors.add(root);
		}
		return ancestors;
	}

	/**
	 * Says if this is an ancestor of descendant
	 *
	 * @param descendant the node that might be descendant of this
	 * @return true if this is an ancestor of descendant, false otherwise
	 */
	public boolean isAncestor(StringGraphNode<?> descendant) {
		return isProperAncestor(descendant) || this.equals(descendant);
	}

	/**
	 * Says if this is a proper ancestor of descendant
	 *
	 * @param descendant the node that might be descendant of this
	 * @return true if this is a proper ancestor of descendant, false otherwise
	 */
	public boolean isProperAncestor(StringGraphNode<?> descendant) {
		return SGNUtils.getForwardPath(this, descendant).size() > 0;
	}

	public Set<StringGraphNode<?>> is() {
		return this.is;
	}

	public Set<StringGraphNode<?>> ris() {
		return this.is.stream().filter(n -> ConstValues.MAX != n.getValue()).collect(Collectors.toSet());
	}
}
