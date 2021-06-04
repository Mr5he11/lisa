package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import javassist.Loader;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class StringGraphNode<V> implements Serializable {

	protected V value;
	protected final List<StringGraphNode<?>> forwardNodes;
	protected final List<StringGraphNode<?>> backwardNodes;
	protected final List<StringGraphNode<?>> forwardParents;
	protected final List<StringGraphNode<?>> backwardParents;
	
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
    	return this.forwardParents.size() + this.backwardParents.size();
    }

    public boolean isLeaf() {
        return this.getOutDegree() == 0;
    }

    public boolean isRoot() {
        return this.forwardParents.isEmpty();
    }

	public List<StringGraphNode<?>> getForwardNodes() {
		return forwardNodes;
	}

	public List<StringGraphNode<?>> getBackwardNodes() {
		return backwardNodes;
	}

	public List<StringGraphNode<?>> getForwardParents() {
		return forwardParents;
	}

	public List<StringGraphNode<?>> getBackwardParents() {
		return backwardParents;
	}

	public <C extends StringGraphNode<?>> void addForwardChild(C child) {
    	this.forwardNodes.add(child);
        child.addForwardParent(this);
    }

	public <C extends StringGraphNode<?>> void addForwardChild(int index, C child) {
		this.forwardNodes.add(index, child);
		child.addForwardParent(this);
	}

	public <C extends StringGraphNode<?>> void addBackwardChild(C child) {
    	this.backwardNodes.add(child);
    	child.addBackwardParent(this);
	}

    protected <P extends StringGraphNode<?>> void addForwardParent(P parent) {
    	this.forwardParents.add(parent);
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
    	this.forwardParents.remove(parent);
    	this.backwardParents.remove(parent);
    }

	public List<StringGraphNode<?>> getChildren() {
		return Stream.concat(getForwardNodes().stream(), getBackwardNodes().stream()).distinct().collect(Collectors.toList());
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
	 * Compacts the given string graph, modifying current String graph instance.
	 * Compaction rules are the following:
	 * <ul>
	 *     <li>no nodes with empty denotation must be present, nodes with empty denotation should be removed</li>
	 *     <li>each OR node has strictly more than one child, otherwise it can be replaced with its child.</li>
	 *     <li>Each child of an OR node should not be a MAX child,otherwise the entire OR subgraph
	 *     can be replaced with a MAX node</li>
	 *     <li>if a forward arc connects two OR nodes, the child node mast have an in-degree > 1,
	 *     otherwise it should be removed, assigning its children to the parent node</li>
	 *     <li>Each cycle should have at least one functor node (OR or CONCAT)
	 *     otherwise the whole cycle can be removed</li>
	 * </ul>
	 * Note that the last rule is always implicitly satisfied because simple and const nodes are not allowed to have
	 * children, so a cycle will always have at least one functor node.
	 */

	/**
	 * Normalizes the String graph having root in the current node. In order to do so, it firstly compacts the String
	 * graph and then applies the following rules:
	 * <ul>
	 *     <li><strong>Rule 1:</strong> Concat nodes with only one child should be replaced with their only child</li>
	 *     <li><strong>Rule 2:</strong> Concat nodes with only MAX node children should be replaced with a MAX node</li>
	 *     <li><strong>Rule 3:</strong> If two concat nodes are successive children of a concat parent, these two
	 *     nodes should be replaced with a single node having, as children, the children of the two replaced nodes
	 *     placed in the same order they were before (only if both the replaced nodes had only one parent)</li>
	 *     <li><strong>Rule 4:</strong> If a concat node has a concat parent, and its in-degree is 1, then it should
	 *     be replaced by its children</li>
	 * </ul>
	 * At the time of writing (2021-05-21) only Concat nodes have specific operations to execute in order to normalize.
	 * <strong>NB:</strong> Rule 4 incorporates rule 3, since the action described in rule 3 will completely be
	 * overwritten by rule 4.
	 */
	public void normalize() {
		this.compact();
		for (StringGraphNode<?> child : this.getForwardNodes())
			child.normalize();
		this.normalizeAux();
	}

	/**
	 * Utility function to be override in each subclass to apply specific behaviour
	 */
	protected void normalizeAux() { }

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
	public List<StringGraphNode<?>> getPrincipalNodes() {
		return List.of(this);
	}

	/**
	 * Static method, replaces one node with another, preserving all relationships
	 *
	 * @param original the node to be replaced
	 * @param replacement the node to insert in place of original
	 */
	public static void replaceNode(StringGraphNode<?> original, StringGraphNode<?> replacement) {
		List<StringGraphNode<?>> forwardNodes = original.getForwardNodes();
		for (int i = forwardNodes.size()-1; i>=0; i--) {
			StringGraphNode<?> child = forwardNodes.get(i);
			replacement.addForwardChild(child);
			original.removeChild(child);
		}
		List<StringGraphNode<?>> backwardNodes = original.getBackwardNodes();
		for (int i = backwardNodes.size()-1; i>=0; i--) {
			StringGraphNode<?> child = backwardNodes.get(i);
			replacement.addForwardChild(child);
			original.removeChild(child);
		}

		List<StringGraphNode<?>> forwardParents = original.getForwardParents();
		for (int i = forwardParents.size()-1; i>=0; i--) {
			StringGraphNode<?> parent = forwardParents.get(i);
			parent.addForwardChild(replacement);
			parent.removeChild(original);
		}

		List<StringGraphNode<?>> backwardParents = original.getBackwardParents();
		for (int i = backwardParents.size()-1; i>=0; i--) {
			StringGraphNode<?> parent = backwardParents.get(i);
			parent.addBackwardChild(replacement);
			parent.removeChild(original);
		}
	}

	/**
	 * Creates a deep copy of the current node, cloning all descendant objects
	 * Credits to <a href="https://alvinalexander.com">Alvin Alexander</a>
	 *
	 * @param node The node to be cloned
	 * @return the cloned object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static StringGraphNode<?> deepClone(StringGraphNode<?> node) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
		objectOutputStream.writeObject(node);
		ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
		ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
		return (StringGraphNode<?>) objectInputStream.readObject();
	}

	public static SimpleStringGraphNode getSingleCharacterString(StringGraphNode<?> node) {
		if (node instanceof SimpleStringGraphNode) {
			return (SimpleStringGraphNode)node;
		}
		if (node instanceof ConcatStringGraphNode) {
			List<StringGraphNode<?>> children = node.getChildren();
			if (
				node.getOutDegree() == 3 &&
				"\"".equals(children.get(0).getValue()) &&
				"\"".equals(children.get(2).getValue())
			) {
				return getSingleCharacterString(children.get(1));
			}

			if (node.getOutDegree() == 1) {
				return getSingleCharacterString(children.get(0));
			}
		}

		return null;
	}
}
