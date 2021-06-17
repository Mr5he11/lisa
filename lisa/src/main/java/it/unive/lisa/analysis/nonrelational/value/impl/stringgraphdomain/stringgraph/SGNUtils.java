package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConstStringGraphNode.ConstValues;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public abstract class SGNUtils {

    /**
     * Type graph compaction algorithm, taken from
     * <a href="https://lirias.kuleuven.be/bitstream/123456789/132040/1/cw108.pdf">
     *     Deriving descriptions of possible values of program variables by means of abstract interpretation: definitions and proofs.
     *     G. Jannsens, M. Bruynooghe, Report CW 108, April 1990
     * </a>
     * Algorithm 4.1.
     *
     * @param node The node to be compacted
     * @return a StringGraphNode, result of the compaction of <code>node</code>
     */
    public static StringGraphNode<?> compact(StringGraphNode<?> node) {
        if (node == null)
            return null;

        StringGraphNode<?> newNode = deepClone(node);
        if (newNode != null) {
            int hash;
            do {
                hash = newNode.hashCode();
                newNode = compactAux(newNode);
            } while (newNode != null && !(newNode.hashCode() == hash));
            return newNode;
        } else {
            return null;
        }
    }

    private static StringGraphNode<?> compactAux(StringGraphNode<?> node) {
        // The algorithm is bottom up, starting from the leaves, so it recursively gets to the leaves first
        List<StringGraphNode<?>> children = new ArrayList<>(node.getForwardNodes());
        for (StringGraphNode<?> child : children) {
            StringGraphNode<?> newChild = compactAux(child);
            if (newChild != child) {
                node.removeChild(child);
                if (newChild != null) {
                    node.addForwardChild(newChild);
                }
            }
        }

        // After compacting all leaves, the node itself can be compacted

        // Rule 1
        if ((node instanceof OrStringGraphNode || node instanceof ConcatStringGraphNode) && node.getDenotation().size() == 0) {
            removeProperDescendants(node);
            return new ConstStringGraphNode(ConstValues.MIN);
        }

        // Rule 4
        if (node instanceof OrStringGraphNode && node.getOutDegree() == 0)
            return new ConstStringGraphNode(ConstValues.MIN);

        // Rule 5
        if (node instanceof OrStringGraphNode && node.getForwardNodes().stream().anyMatch(c -> c.value == ConstValues.MAX)) {
            removeProperDescendants(node);
            return new ConstStringGraphNode(ConstValues.MAX);
        }

        // Rule 7
        if (node instanceof OrStringGraphNode && node.getOutDegree() == 1) {
            StringGraphNode<?> child = node.getBackwardNodes().size() == 1 ? node.getBackwardNodes().get(0) : node.getForwardNodes().get(0);
            StringGraphNode<?> forwardParent = node.getForwardParent();
            forwardParent.removeChild(node);
            forwardParent.addForwardChild(child);

            List<StringGraphNode<?>> backwardParents = new ArrayList<>(node.getBackwardParents());
            for (StringGraphNode<?> parent : backwardParents) {
                parent.removeChild(node);
                parent.addBackwardChild(child);
            }
            node.removeChild(child);
            return null;
        }

        // Rule 8
        if (node.getBackwardNodes().size() > 0) {
            List<StringGraphNode<?>> backwardChildren = new ArrayList<>(node.getBackwardNodes());
            for (StringGraphNode<?> child : backwardChildren) {
                List<StringGraphNode<?>> forwardPath = getForwardPath(child, node);
                if (forwardPath.size() > 0 && forwardPath.stream().allMatch(k -> k instanceof OrStringGraphNode)) {
                    for (StringGraphNode<?> k : forwardPath) {
                        List<StringGraphNode<?>> kBackwardParents = new ArrayList<>(k.getBackwardParents());
                        for (StringGraphNode<?> l : kBackwardParents) {
                            l.removeChild(k);
                            l.addBackwardChild(child);
                        }
                    }
                    return node;
                }
            }
        }

        if (node instanceof OrStringGraphNode) {
            // Rule 2
            List<StringGraphNode<?>> toBeRemoved = node.getForwardNodes().stream()
                    .filter(c -> c.value == ConstValues.MIN)
                    .collect(Collectors.toList());
            for (StringGraphNode<?> child : toBeRemoved)
                node.removeChild(child);

            // Rule 3
            if (node.getForwardNodes().contains(node) || node.getBackwardNodes().contains(node)) {
                node.removeChild(node);
            }

            // Rule 6
            toBeRemoved = node.getForwardNodes().stream()
                    .filter(c -> (c instanceof OrStringGraphNode && c.getInDegree() == 1))
                    .collect(Collectors.toList());

            for (StringGraphNode<?> child : toBeRemoved) {
                node.removeChild(child);
                List<StringGraphNode<?>> childForwardChildren = new ArrayList<>(child.getForwardNodes());
                List<StringGraphNode<?>> childBackwardChildren = new ArrayList<>(child.getBackwardNodes());
                for (StringGraphNode<?> c : childForwardChildren) {
                    child.removeChild(c);
                    node.addForwardChild(c);
                }
                for (StringGraphNode<?> c : childBackwardChildren) {
                    child.removeChild(c);
                    node.addForwardChild(c);
                }
            }

            return node;
        }

        return node;
    }

    /**
     * Removes proper descendants of a node, IE those children that do not have any parent but the given node or one of
     * its proper descendants.
     *
     * @param node The node from which to remove the proper descendants
     */
    private static void removeProperDescendants(StringGraphNode<?> node) {
        List<StringGraphNode<?>> toBeRemoved = new ArrayList<>(node.getForwardNodes());
        for (StringGraphNode<?> child : toBeRemoved) {
            // remove child's children only if child does not have other parents
            if (child.getInDegree() == 1)
                removeProperDescendants(child);
            node.removeChild(child);
        }
    }

    /**
     * Creates a deep copy of the current node, cloning all descendant objects
     * Credits to <a href="https://alvinalexander.com">Alvin Alexander</a>
     *
     * @param node The node to be cloned
     * @return the cloned object
     */
    public static StringGraphNode<?> deepClone(StringGraphNode<?> node) {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
            objectOutputStream.writeObject(node);
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(byteOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
            return (StringGraphNode<?>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
            return null;
        }
    }

    /**
     * Computes the forward path that stays between a parent and a child node.
     * The return format is an array of nodes where the node at 0 is the targeted child and
     * the last node of the list is the ancestor of the provided child that comes after the parent itself.
     * If there is no path between the two given nodes,
     * the function returns an empty list.
     *
     * @param parent the ancestor of #child
     * @param child the descendant of #parent
     * @return a list the nodes in the path between #parent and #child, #parent excluded #child included, if found,
     *  an empty list otherwise
     */
    public static List<StringGraphNode<?>> getForwardPath(StringGraphNode<?> parent, StringGraphNode<?> child) {
        if (parent.getForwardNodes().stream().anyMatch(c -> c.toString().equals(child.toString()))) {
            return Collections.singletonList(child);
        } else {
            for (StringGraphNode<?> c : parent.getForwardNodes()) {
                List<StringGraphNode<?>> result = getForwardPath(c, child);
                if (result.size() > 0) {
                    List<StringGraphNode<?>> returnValue = new ArrayList<>(result);
                    returnValue.add(c);
                    return returnValue;
                }
            }
        }
        return new ArrayList<>();
    }


    /**
     * Auxiliary function for implementing partial ordering.
     * Following Section 4.4.2, this recursive algorithm checks the denotations of two nodes and returns true if
     * the denotation of node n is contained or equal to the denotation of node m.
     * The cases implemented in this function are the following:
     * <ul>
     *     <li><strong>Case 1:</strong> if the edge that links the two roots is already in the set, return true</li>
     *     <li><strong>Case 2:</strong> if the label of the right root is NAX, return true</li>
     *     <li><strong>Case 3:</strong> if the two roots are concat node, with same length k, check for each child if the property holds,
     *     		while adding to the current set of edges a new edge formed by the current nodes n and m.</li>
     *     <li><strong>Case 4:</strong> if the two roots are OR node, take all the children of the left node n and check if the property holds for each of them,
     *     		comparing with the root m, while adding to the current set of edges a new edge formed by the current nodes n and m.</li>
     *     <li><strong>Case 5:</strong> if the right root m is an OR node, look for a node in the Principal Nodes of m which has the same label as the label of n.
     *     		Then check if the property holds for n and this particular node, adding to the current set of edge the edge (n,m)</li>
     *     <li><strong>Case 6:</strong> last check, if none of the previous checks has passed, is to verify that the label of node n is equal to the label of node m.</li>
     * </ul>
     *
     * @param n the root of the graph tree to compare
     * @param m the root of the other graph tree to compare
     * @param edges set of edges represented with the Map.Entry entity
     * @return true if n is less or equal then m, false otherwise
     */
    public static boolean partialOrderAux(StringGraphNode<?> n, StringGraphNode<?> m, Set<Map.Entry<StringGraphNode<?>, StringGraphNode<?>>> edges) {

        // case (1)
        if (edges.contains(StringGraphNode.createEdge(n, m))) {
            return true;
        }

        // case (2)
        else if (m instanceof ConstStringGraphNode) {
            ConstValues constValue = ((ConstStringGraphNode)m).getValue();

            if (ConstValues.MAX == constValue) {
                return true;
            }
        }

        // case (3)
        else if (n instanceof ConcatStringGraphNode && m instanceof ConcatStringGraphNode) {
            ConcatStringGraphNode concatN = (ConcatStringGraphNode)n;
            ConcatStringGraphNode concatM = (ConcatStringGraphNode)m;

            List<StringGraphNode<?>> childrenN = n.getChildren();
            List<StringGraphNode<?>> childrenM = m.getChildren();

            if (Objects.equals(concatN.getValue(), concatM.getValue())) {

                // add current edge to edgeSet
                edges.add(StringGraphNode.createEdge(n, m));

                // for each i in [0,k] must hold: <=(n/i, m/i, edges+{m,n})
                for (int i=0; i<concatN.getValue(); i++) {

                    boolean isLessOrEqual = partialOrderAux(childrenN.get(i), childrenM.get(i), edges);

                    if (!isLessOrEqual) return false;
                }

                return true;
            }
        }

        // case (4)
        else if (n instanceof OrStringGraphNode && m instanceof OrStringGraphNode) {
            int k = n.getOutDegree();
            List<StringGraphNode<?>> children = n.getChildren();

            // add current edge to edgeSet
            edges.add(StringGraphNode.createEdge(n, m));

            // for each i in [0,k] must hold: <=(n/i, m, edges+{m,n})
            for (int i=0; i<k; i++) {

                boolean isLessOrEqual = partialOrderAux(children.get(i), m, edges);

                if (!isLessOrEqual) return false;
            }

            return true;

        }

        // case (5)
        else if (m instanceof OrStringGraphNode) {

            StringGraphNode<?> md = null;

            // look for a node (named md) in prnd(m) such that lb(n) == lb(md)
            for (StringGraphNode<?> prnd: m.getPrincipalNodes()) {
                String prndLbl = prnd.getLabel();
                if ( (prndLbl == null && n.getLabel() == null) ||
                        (prndLbl != null && prnd.getLabel().equals(n.getLabel()))) {

                    md = prnd; // found one
                    break;
                }
            }


            if (md != null) {
                edges.add(StringGraphNode.createEdge(n, m));
                return partialOrderAux(n, md, edges);
            }

        }

        // case (6)
        return (n.getLabel().equals( m.getLabel() ));
    }


    /**
     * Auxiliary function for implementing Contains.
     * Following Section 4.4.6 - Table VII, this recursive algorithm checks whether there is a Concat Node with a child node
     * which label is equal to the second argument of the function
     *
     * @param node the root of the graph tree of interest
     * @param c the character which has to be contained in the graph tree with root node
     * @return true iff there is a concat node with a node labeled 'c'
     */
    public static boolean strContainsAux_checkTrue(StringGraphNode<?> node, Character c) {

        if (node instanceof SimpleStringGraphNode) {
            return ((SimpleStringGraphNode) node).getValueAsChar().equals(c);
        }

        // do not check OR nodes here! At first iteration, left is the root, which can be an OR node!

        List<StringGraphNode<?>> children = node.getChildren();
        for (StringGraphNode<?> child: children) {

            if (child instanceof OrStringGraphNode) {
                continue;
            }

            if (child instanceof  SimpleStringGraphNode) {
                if (strContainsAux_checkTrue(child, c)) {
                    return true;
                }
            }

            if (child instanceof ConcatStringGraphNode) {
                Integer k = ((ConcatStringGraphNode)child).getValue();
                for (int i=0; i<k; i++) {

                    if (strContainsAux_checkTrue(child, c)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Auxiliary function for implementing Contains.
     * Following Section 4.4.6 - Table VII, this recursive algorithm checks whether there is no node which is labelled with the character 'c' or the constant MAX
     *
     * @param node the root of the graph tree of interest
     * @param c the character which has NOT to be contained in the graph tree with root node
     * @return true iff there is no node labelled 'c' or MAX
     */
    public static boolean strContainsAux_checkFalse(StringGraphNode<?> node, Character c) {

        if (node instanceof ConstStringGraphNode) {
            return !ConstValues.MAX.equals( ((ConstStringGraphNode)node).getValue() );
        }

        if (node instanceof SimpleStringGraphNode) {
            return !((SimpleStringGraphNode) node).getValueAsChar().equals(c);
        }

        // for all children must hold that: no child is c or MAX
        for (StringGraphNode<?> child: node.getChildren()) {
            if (!strContainsAux_checkFalse(child, c)) {
                return false;
            }
        }

        // survived the check
        return true;
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
