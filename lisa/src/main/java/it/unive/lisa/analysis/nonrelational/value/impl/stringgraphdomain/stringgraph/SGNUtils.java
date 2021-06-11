package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

        StringGraphNode<?> oldNode = node;
        StringGraphNode<?> newNode = compactAux(oldNode);
        while (newNode != null && !(newNode.toString().equals(oldNode.toString()))) {
            oldNode = newNode;
            newNode =  compactAux(oldNode);
        }
        return newNode;
    }

    private static StringGraphNode<?> compactAux(StringGraphNode<?> node) {
        boolean isModified = false;

        // The algorithm is bottom up, starting from the leaves, so it recursively gets to the leaves first
        List<StringGraphNode> children = new ArrayList<>(node.getForwardNodes());
        for (StringGraphNode child : children) {
            StringGraphNode<?> newChild = compactAux(child);
            if (newChild != child) {
                isModified = true;
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
            List<StringGraphNode<?>> forwardParents = new ArrayList<>(node.getForwardParents());
            List<StringGraphNode<?>> backwardParents = new ArrayList<>(node.getBackwardParents());
            for (StringGraphNode<?> parent : forwardParents) {
                parent.removeChild(node);
                parent.addForwardChild(child);
            }
            for (StringGraphNode<?> parent : backwardParents) {
                parent.removeChild(node);
                parent.addBackwardChild(child);
            }
            node.removeChild(child);
            return null;
        }

        // Rule 8
        // TODO: must do rule 8

        if (node instanceof OrStringGraphNode) {
            // Rule 2
            List<StringGraphNode<?>> toBeRemoved = node.getForwardNodes().stream().filter(c -> c.value == ConstValues.MIN).collect(Collectors.toList());
            for (StringGraphNode<?> child : toBeRemoved)
                node.removeChild(child);

            // Rule 3
            if (node.getForwardNodes().contains(node) || node.getBackwardNodes().contains(node)) {
                node.removeChild(node);
            }

            // Rule 6
            toBeRemoved = node.getForwardNodes().stream().filter(c -> {
                return (c instanceof OrStringGraphNode && c.getInDegree() == 1);
            }).collect(Collectors.toList());
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

            return deepClone(node);
        }

        return isModified ? deepClone(node) : node;
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
     * The return format is an array of nodes where the node at 0 is the parent of the targeted child and
     * the last node of the list is the provided parent itself. If there is no path between the two given nodes,
     * the function returns an empty list.
     *
     * @param parent the ancestor of #child
     * @param child the descendant of #parent
     * @return a list of ancestors of #child if there is a path, [] otherwise
     */
    public static List<StringGraphNode<?>> getForwardPath(StringGraphNode<?> parent, StringGraphNode<?> child) {
        if (parent.getForwardNodes().stream().anyMatch(c -> c.toString().equals(child.toString()))) {
            return Collections.singletonList(parent);
        } else {
            for (StringGraphNode<?> c : parent.getForwardNodes()) {
                List<StringGraphNode<?>> result = getForwardPath(c, child);
                if (result.size() > 0 && result.get(result.size() - 1).toString().equals(c.toString())) {
                    List<StringGraphNode<?>> returnValue = new ArrayList<>(result);
                    returnValue.add(parent);
                    return returnValue;
                }
            }
        }
        return new ArrayList<>();
    }
}
