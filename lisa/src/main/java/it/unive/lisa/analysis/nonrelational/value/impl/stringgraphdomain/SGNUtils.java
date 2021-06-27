package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.*;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.ConstStringGraphNode.ConstValues;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;


public abstract class SGNUtils {

    //==================================== COMPACTION SECTION =========================================================

    /**
     * Type graph compaction algorithm, taken from
     * <a href="https://lirias.kuleuven.be/bitstream/123456789/132040/1/cw108.pdf">
     *     Deriving descriptions of possible values of program variables by means of abstract interpretation: definitions and proofs.
     *     G. Jannsens, M. Bruynooghe, Report CW 108, April 1990
     * </a>
     * Algorithm 4.1.
     *
     * Moreover, there are some additional rules to apply in the case of string graphs, concerning the particular
     * case of the functor nodes CONCAT/k. Such rules can be found in the paper
     * <a href="https://onlinelibrary.wiley.com/doi/full/10.1002/spe.2218">
     *      A suite of abstract domains for static analysis of string values
     *      Giulia Costantini, Pietro Ferrara, Agostino Cortesi
     * </a> at section 4.4.1.
     * In this context such restrictions are applied to normal string graphs, but by the fact that a
     * normal string graph is a particular case of a compact string graph, and since compaction algorithm is not applied
     * alone anywhere, it is safe here to apply such restrictions in the compaction process. Moreover, these restrictions
     * are more a matter of compaction then of normalization, since they do not concern the definition of
     * "principal label restriction".
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
        List<StringGraphNode<?>> children = new ArrayList<>(node.getForwardChildren());
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
        if (node instanceof OrStringGraphNode && node.getForwardChildren().stream().anyMatch(c -> c.getValue() == ConstValues.MAX)) {
            removeProperDescendants(node);
            return new ConstStringGraphNode(ConstValues.MAX);
        }

        // Rule 7
        if (node instanceof OrStringGraphNode && node.getOutDegree() == 1) {
            StringGraphNode<?> child = node.getBackwardChildren().size() == 1 ? node.getBackwardChildren().get(0) : node.getForwardChildren().get(0);
            StringGraphNode<?> forwardParent = node.getForwardParent();
            List<StringGraphNode<?>> backwardParents = new ArrayList<>(node.getBackwardParents());
            node.removeChild(child);

            for (StringGraphNode<?> parent : backwardParents) {
                parent.removeChild(node);
                int nodeIndex = parent.getChildren().indexOf(node);
                parent.addBackwardChild(nodeIndex, child);
            }

            if (forwardParent != null) {
                forwardParent.removeChild(node);
                forwardParent.addForwardChild(child);
                return null;
            } else {
                return child;
            }
        }

        // Rule 8
        if (node.getBackwardChildren().size() > 0) {
            List<StringGraphNode<?>> backwardChildren = new ArrayList<>(node.getBackwardChildren());
            for (StringGraphNode<?> child : backwardChildren) {
                List<StringGraphNode<?>> forwardPath = getForwardPath(child, node);
                if (forwardPath.size() > 0 && forwardPath.stream().allMatch(k -> k instanceof OrStringGraphNode)) {
                    for (StringGraphNode<?> k : forwardPath) {
                        List<StringGraphNode<?>> kBackwardParents = new ArrayList<>(k.getBackwardParents());
                        for (StringGraphNode<?> l : kBackwardParents) {
                            int kIndex = l.getChildren().indexOf(k);
                            l.removeChild(k);
                            l.addBackwardChild(kIndex, child);
                        }
                    }
                    return node;
                }
            }
        }

        // Specific rules for or nodes
        if (node instanceof OrStringGraphNode) {
            // Rule 2
            List<StringGraphNode<?>> toBeRemoved = node.getForwardChildren().stream()
                    .filter(c -> c.getValue() == ConstValues.MIN)
                    .collect(Collectors.toList());
            for (StringGraphNode<?> child : toBeRemoved)
                node.removeChild(child);

            // Rule 3
            if (node.getForwardChildren().contains(node) || node.getBackwardChildren().contains(node)) {
                node.removeChild(node);
            }

            // Rule 6
            toBeRemoved = node.getForwardChildren().stream()
                    .filter(c -> (c instanceof OrStringGraphNode && c.getInDegree() == 1))
                    .collect(Collectors.toList());

            for (StringGraphNode<?> child : toBeRemoved) {
                node.removeChild(child);
                List<StringGraphNode<?>> childForwardChildren = new ArrayList<>(child.getForwardChildren());
                List<StringGraphNode<?>> childBackwardChildren = new ArrayList<>(child.getBackwardChildren());
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

        // Additional rules for concat nodes, as explained in the java doc of this function
        if (node instanceof ConcatStringGraphNode) {
            // Rule 1 from paper A suite of abstract domains for static analysis of string values - 4.4.1
            if (node.getOutDegree() == 1) {
                StringGraphNode<?> newNode = node.getChildren().get(0);
                List<StringGraphNode<?>> nodeBackwardParents = new ArrayList<>(node.getBackwardParents());
                for (StringGraphNode<?> parent : nodeBackwardParents) {
                    int nodeIndex = parent.getChildren().indexOf(node);
                    parent.removeChild(node);
                    parent.addBackwardChild(nodeIndex, newNode);
                }
                node.removeChild(newNode);
                return newNode;
            }

            // Rule 2 from paper A suite of abstract domains for static analysis of string values - 4.4.1
            if (
                node.getBackwardChildren().isEmpty() &&
                node.getForwardChildren().stream().allMatch(n -> n instanceof ConstStringGraphNode && n.getValue().equals(ConstValues.MAX))
            ) {
                return new ConstStringGraphNode(ConstValues.MAX);
            }

            // Rule 3/4 from paper A suite of abstract domains for static analysis of string values - 4.4.1
            int index = 0;
            while (index < node.getChildren().size()) {
                StringGraphNode<?> child = node.getChildren().get(index);
                if (child instanceof ConcatStringGraphNode && child.getInDegree() <= 1) {
                    while (child.getChildren().size() > 0) {
                        StringGraphNode<?> c = child.getChildren().get(0);
                        node.addForwardChild(index, c);
                        child.removeChild(c);
                        index += 1;
                    }
                    node.removeChild(child);
                } else {
                    index += 1;
                }
            }
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
        List<StringGraphNode<?>> toBeRemoved = new ArrayList<>(node.getForwardChildren());
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
        StringGraphNode<?> node = child;
        List<StringGraphNode<?>> path = new ArrayList<>();
        while (!node.isRoot() && !node.equals(parent)) {
            path.add(node);
            node = node.getForwardParent();
        }
        return node.equals(parent) ? path : new ArrayList<>();
    }

    //==================================== DOMAIN UTILS SECTION =======================================================

    /**
     * Auxiliary function for implementing partial ordering.
     * Following Section 4.4.2, this recursive algorithm checks the denotations of two nodes and returns true if
     * the denotation of node n is contained or equal to the denotation of node m.
     * The cases implemented in this function are the following:
     * <ul>
     *     <li><strong>Case 1:</strong> if the edge that links the two roots is already in the set, return true</li>
     *     <li><strong>Case 2:</strong> if the label of the right root is MAX, return true</li>
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
        if (edges.contains(StringGraphNode.createEdge(n, m)))
            return true;

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

        if (node instanceof SimpleStringGraphNode)
            return ((SimpleStringGraphNode) node).getValueAsChar().equals(c);

        // do not check OR nodes here! At first iteration, left is the root, which can be an OR node!
        List<StringGraphNode<?>> children = node.getChildren();
        for (StringGraphNode<?> child: children) {
            if (child instanceof OrStringGraphNode) continue;
            if (child instanceof  SimpleStringGraphNode)
                if (strContainsAux_checkTrue(child, c))
                    return true;
            if (child instanceof ConcatStringGraphNode) {
                Integer k = ((ConcatStringGraphNode)child).getValue();
                for (int i=0; i<k; i++)
                    if (strContainsAux_checkTrue(child, c))
                        return true;
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

        if (node instanceof ConstStringGraphNode)
            return !ConstValues.MAX.equals( ((ConstStringGraphNode)node).getValue() );
        if (node instanceof SimpleStringGraphNode)
            return !((SimpleStringGraphNode) node).getValueAsChar().equals(c);

        // for all children must hold that: no child is c or MAX
        for (StringGraphNode<?> child: node.getChildren())
            if (!strContainsAux_checkFalse(child, c))
                return false;

        // survived the check
        return true;
    }

    //==================================== UTILS SECTION ==============================================================

    /**
     * If the argument node has a single character or is a concat with three characters, with the first and the third
     * being '"', returns a simple node with the character as value. Otherwise, returns null;
     *
     * @param node a node that has to be transformed into a single character node
     * @return the single character simple node
     */
    public static SimpleStringGraphNode getSingleCharacterString(StringGraphNode<?> node) {
        if (node instanceof SimpleStringGraphNode)
            return (SimpleStringGraphNode)node;
        if (node instanceof ConcatStringGraphNode) {
            List<StringGraphNode<?>> children = node.getChildren();
            if (node.getOutDegree() == 3
                    && "\"".equals(children.get(0).getValue())
                    && "\"".equals(children.get(2).getValue()))
                return getSingleCharacterString(children.get(1));
            if (node.getOutDegree() == 1)
                return getSingleCharacterString(children.get(0));
        }
        return null;
    }

    /**
     * Given a string, returns the same string stripping the initial and final quotation marks if present
     *
     * @param  value the initial string to be unquoted
     * @return the unquoted string
     * */
    public static String unquote(String value) {
        if (value == null)
            return "";
        if (value.startsWith("\""))
            value = value.substring(1);
        if (value.endsWith("\""))
            value = value.substring(0, value.length()-1);
        return value;
    }

    /**
     * Replaces a StrigGraphNode with another instance of StringGraphNode
     *
     * @param original the node to be replaced
     * @param replacement the node that should take the place of original
     */
    public static void replace(StringGraphNode<?> original, StringGraphNode<?> replacement) {
        if (!(original == replacement)) {
            List<StringGraphEdge> originalEdges = new ArrayList<>(original.getChildrenEdges());
            List<StringGraphNode<?>> originalBackwardParents = new ArrayList<>(original.getBackwardParents());
            for (StringGraphEdge e : originalEdges) {
                original.removeChild(e.getNode());
            }
            for (StringGraphNode<?> backwardParent : originalBackwardParents) {
                int originalIndex = backwardParent.getChildren().indexOf(original);
                backwardParent.addBackwardChild(originalIndex, replacement);
                backwardParent.removeChild(original);
            }
            if (!original.isRoot()) {
                StringGraphNode<?> forwardParent = original.getForwardParent();
                int originalIndex = forwardParent.getChildren().indexOf(original);
                forwardParent.addForwardChild(originalIndex, replacement);
                forwardParent.removeChild(original);
            }
        }
    }

    //==================================== NORMALIZATION SECTION ======================================================

    /**
     * Normalization algorithm taken from <a href="https://lirias.kuleuven.be/bitstream/123456789/132040/1/cw108.pdf">
     * Deriving descriptions of possible values of program variables by means of abstract interpretation: definitions and proofs.
     * G. Jannsens, M. Bruynooghe, Report CW 108, April 1990 </a>.
     * The algorithm 4.2 of such paper describes the steps to take in order to obtain a restricted type graph.
     * We omitted the branch 4.a, since its purpose was to assure that the resulted graph didn't violate the depth restriction.
     * We do not need the latter property.
     *
     * @param node the node to be normalized
     * @return the normalized node
     */
    public static StringGraphNode<?> normalize(StringGraphNode<?> node) {
        try {
            /* INITIALIZATION */
            HashMap<String, Set<StringGraphNode<?>>> idToNfr = new HashMap<>();
            HashMap<String, Set<StringGraphNode<?>>> idToNd = new HashMap<>();
            StringGraphNode<?> m0 = initializeNodeFromSource(node);
            Set<StringGraphNode<?>> S_ul = new LinkedHashSet<>();
            Set<StringGraphNode<?>> S_sn = new LinkedHashSet<>();
            S_ul.add(m0);

            if (m0 instanceof  OrStringGraphNode)
                idToNfr.put(m0.id, new LinkedHashSet<>(node.getChildren()));
            else
                idToNfr.put(m0.id, new LinkedHashSet<>());
            idToNd.put(m0.id, new LinkedHashSet<>(idToNfr.get(m0.id)));
            idToNd.get(m0.id).add(node);

            /* REPEAT UNTIL */
            do {
                StringGraphNode<?> m = S_ul.iterator().next();

                Optional<StringGraphNode<?>> mgOpt = S_sn.stream().filter(_mg -> safeAnc(m, idToNd, _mg)).findFirst();
                if (mgOpt.isPresent()) {
                    // CASE 1
                    StringGraphNode<?> mg = mgOpt.get();
                    ulBarc(m, mg, S_ul);
                } else if (m instanceof SimpleStringGraphNode
                        || (m instanceof ConcatStringGraphNode
                        && ((ConcatStringGraphNode)m).getValue() == 0
                        && ((ConcatStringGraphNode)m).desiredNumberOfChildren == 0)) {
                    // CASE 2
                    S_ul.remove(m);
                    S_sn.add(m);
                } else if (m instanceof OrStringGraphNode) {
                    // CASE 3
                    Optional<StringGraphNode<?>> nOpt = idToNfr.get(m.id).stream().filter(_n -> involvedOverlap(m,_n, idToNfr)).findFirst();
                    while(nOpt.isPresent()) {
                        StringGraphNode<?> n = nOpt.get();
                        idToNfr.get(m.id).remove(n);
                        idToNfr.get(m.id).addAll(n.getChildren().stream().filter(k -> !idToNd.get(m.id).contains(k)).collect(Collectors.toSet()));
                        idToNd.get(m.id).addAll(n.getChildren());
                        nOpt = idToNfr.get(m.id).stream().filter(_n -> involvedOverlap(m,_n, idToNfr)).findFirst();
                    }
                    mgOpt = S_sn.stream().filter(_mg -> safeAnc(m, idToNd, _mg)).findFirst();
                    if (mgOpt.isPresent()) {
                        ulBarc(m, mgOpt.get(), S_ul);
                    } else {
                        for (StringGraphNode<?> n : idToNfr.get(m.id).stream().filter(_n -> _n instanceof  OrStringGraphNode).collect(Collectors.toList())) {
                            StringGraphNode<?> newChild = new OrStringGraphNode();
                            m.addForwardChild(newChild);
                            idToNfr.put(newChild.id, new LinkedHashSet<>(n.getChildren()));
                            idToNd.put(newChild.id, new LinkedHashSet<>(n.getChildren()));
                            idToNd.get(newChild.id).add(n);
                        }
                        Set<String> labels = idToNfr
                                .get(m.id)
                                .stream()
                                .filter(_n -> _n instanceof ConcatStringGraphNode || _n instanceof SimpleStringGraphNode)
                                .map(StringGraphNode::getLabel)
                                .collect(Collectors.toSet());
                        for (String l : labels) {
                            Set<StringGraphNode<?>> sameLabel = idToNfr.get(m.id).stream().filter(_n -> _n.getLabel().equals(l)).collect(Collectors.toSet());
                            StringGraphNode<?> newChild = initializeNodeFromSource(sameLabel.iterator().next());
                            m.addForwardChild(newChild);
                            idToNd.put(newChild.id, sameLabel);
                            idToNfr.put(newChild.id, new LinkedHashSet<>());
                        }
                        S_ul.remove(m);
                        S_ul.addAll(m.getChildren());
                        S_sn.add(m);
                    }
                } else if (m instanceof ConcatStringGraphNode) {
                    // CASE 4
                    if (idToNd.get(m.id).size() == 1) {
                        StringGraphNode<?> n = idToNd.get(m.id).iterator().next();
                        for (int i = 0; i < ((ConcatStringGraphNode) m).desiredNumberOfChildren; i++) {
                            StringGraphNode<?> child = n.getChildren().get(i);
                            StringGraphNode<?> newChild = initializeNodeFromSource(child);
                            m.addForwardChild(newChild);
                            if (newChild instanceof OrStringGraphNode) {
                                idToNfr.put(newChild.id, new LinkedHashSet<>(child.getChildren()));
                            } else {
                                idToNfr.put(newChild.id, new LinkedHashSet<>());
                            }
                            idToNd.put(newChild.id,  new LinkedHashSet<>(idToNfr.get(newChild.id)));
                            idToNd.get(newChild.id).add(child);
                        }
                    } else {
                        for (int i = 0; i < ((ConcatStringGraphNode) m).desiredNumberOfChildren; i++) {
                            int finalI = i;
                            Set<StringGraphNode<?>> iThChildren = idToNd
                                    .get(m.id)
                                    .stream()
                                    .filter(_n -> _n.getOutDegree() > finalI)
                                    .map(_n -> (_n).getChildren().get(finalI))
                                    .collect(Collectors.toSet());
                            if (iThChildren.stream().anyMatch(_i -> _i instanceof ConstStringGraphNode && _i.getValue().equals(ConstValues.MAX))) {
                                StringGraphNode<?> newChild = new ConstStringGraphNode(ConstValues.MAX);
                                m.addForwardChild(newChild);
                                idToNd.put(newChild.id, new LinkedHashSet<>(iThChildren));
                            } else {
                                StringGraphNode<?> newChild = new OrStringGraphNode();
                                m.addForwardChild(newChild);
                                idToNfr.put(newChild.id, new LinkedHashSet<>(iThChildren));
                                idToNd.put(newChild.id, new LinkedHashSet<>(iThChildren));
                            }
                        }
                    }
                    S_ul.remove(m);
                    S_ul.addAll(m.getChildren());
                    S_sn.add(m);
                }

            } while(S_ul.size() > 0);

            return compact(m0);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    /* AUXILIARY FUNCTIONS FOR NORMALIZATION ALGORITHM */

    private static boolean safeAnc(StringGraphNode<?> m, HashMap<String, Set<StringGraphNode<?>>> fn, StringGraphNode<?> mg) {
        List<StringGraphNode<?>> mgToMPath = getForwardPath(mg,m);
        if (mgToMPath.size() > 0) {
            mgToMPath.add(mg);
            mgToMPath.remove(0);
        }
        Set<StringGraphNode> intersection = new HashSet<>(fn.get(mg.id));
        intersection.retainAll(fn.get(m.id));
        return mg.isProperAncestor(m)
                && (intersection.size() == fn.get(m.id).size() && intersection.size() == fn.get(mg.id).size())
                && mgToMPath.stream().anyMatch(mf -> !(mf instanceof OrStringGraphNode));
    }

    private static boolean involvedOverlap(StringGraphNode<?> m, StringGraphNode<?> n, HashMap<String, Set<StringGraphNode<?>>> nfr) {
        Set<String> nPrincipalLabels = n.getPrincipalLabels();
        Set<String> otherPrincipalLabels = nfr.get(m.id).stream().filter(k -> !k.equals(n)).flatMap(k -> k.getPrincipalLabels().stream()).collect(Collectors.toSet());
        Set<String> intersection = new HashSet<>(nPrincipalLabels);
        intersection.retainAll(otherPrincipalLabels);
        return nfr.get(m.id).contains(n) && n instanceof OrStringGraphNode && !(intersection.isEmpty());
    }

    private static void ulBarc(StringGraphNode<?> m, StringGraphNode<?> mg, Set<StringGraphNode<?>> S_ul) {
        S_ul.remove(m);
        StringGraphNode<?> mParent = m.getForwardParent();
        int mIndex = mParent.getChildren().indexOf(m);
        mParent.removeChild(m);
        mParent.addBackwardChild(mIndex, mg);
    }

    private static <T> StringGraphNode<T> initializeNodeFromSource(StringGraphNode<T> source)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        StringGraphNode<T> newNode = (StringGraphNode<T>) source.getClass().getDeclaredConstructor().newInstance();
        if (source instanceof ConcatStringGraphNode) {
            ConcatStringGraphNode result = ((ConcatStringGraphNode)newNode);
            result.desiredNumberOfChildren = (int)source.getValue();
            return (StringGraphNode<T>) result;
        } else {
            newNode.setValue(source.getValue());
            return newNode;
        }
    }

    //========================================= WIDENING SECTION ======================================================

    /**
     * Widening algorithm taken from the appendix of <a href="https://doi.org/10.1145/773473.178479">
     * P. Van Hentenryck, A. Cortesi, and B. Le Charlier. 1994. Type analysis of Prolog using type graphs.
     * SIGPLAN Not. 29, 6 (June 1994)</a>.
     *
     * @param go the root of the old graph
     * @param gn the root of the graph obtained normalizing the or between go and a new provided graph
     * @return the widened graph
     */
    public static StringGraphNode<?> widening(StringGraphNode<?> go, StringGraphNode<?> gn) {
        if (gn != null) {
            /* CYCLE INTRODUCTION RULE */
            Iterator<List<StringGraphNode<?>>> CIIterator = CI(go, gn).iterator();
            if (CIIterator.hasNext()) {
                List<StringGraphNode<?>> CIEl = CIIterator.next();
                StringGraphNode<?> v = CIEl.get(0);
                StringGraphNode<?> vn = CIEl.get(1);
                StringGraphNode<?> va = CIEl.get(3);
                int vnIndex = v.getChildren().indexOf(vn);
                v.removeChild(vn);
                v.addBackwardChild(vnIndex, va);
                return gn;
            } else {
                /* REPLACEMENT RULE */
                Iterator<List<StringGraphNode<?>>> CRIterator = CR(go, gn).iterator();
                if (CRIterator.hasNext()) {
                    List<StringGraphNode<?>> CREl = CRIterator.next();
                    StringGraphNode<?> vn = CREl.get(0);
                    StringGraphNode<?> va = CREl.get(1);
                    replace(va, vn);
                    return gn;
                } else {
                    return gn;
                }
            }
        }
        return go;
    }

    /* AUXILIARY FUNCTIONS FOR WIDENING ALGORITHM */

    private static Set<List<StringGraphNode<?>>> correspondenceSet(StringGraphNode<?> g1, StringGraphNode<?> g2) {
        return correspondenceSetAux(g1, g2);
    }

    private static Set<List<StringGraphNode<?>>> correspondenceSetAux(StringGraphNode<?> g1, StringGraphNode<?> g2) {
        Set<List<StringGraphNode<?>>> relation = new HashSet<>();
        relation.add(List.of(g1,g2));
        if (eDepth(g1,g2) || ePf(g1,g2)) {
            Iterator<StringGraphNode<?>> v1Iterator = g1.getForwardChildren().iterator();
            Iterator<StringGraphNode<?>> v2Iterator = g2.getForwardChildren().iterator();
            while(v1Iterator.hasNext() && v2Iterator.hasNext()) {
                relation.addAll(correspondenceSetAux(v1Iterator.next(), v2Iterator.next()));
            }
        }
        return relation;
    }

    private static Set<List<StringGraphNode<?>>> topologicalClashes(StringGraphNode<?> g1, StringGraphNode<?> g2) {
        if (!eDepth(g1, g2) && ePf(g1, g2)) {
            return correspondenceSet(g1, g2);
        }
        return new HashSet<>();
    }

    private static Set<List<StringGraphNode<?>>> wideningTopologicalClashes(StringGraphNode<?> g1, StringGraphNode<?> g2) {
        return topologicalClashes(g1, g2)
                .stream()
                .filter(pair -> {
                    StringGraphNode<?> v = pair.get(0);
                    StringGraphNode<?> _v = pair.get(1);
                    return (!(_v instanceof SimpleStringGraphNode || _v instanceof ConstStringGraphNode))
                            && (!ePf(v,_v) || v.getDepth() < _v.getDepth());
                })
                .collect(Collectors.toSet());
    }

    private static boolean eDepth(StringGraphNode<?> v1, StringGraphNode<?> v2) {
        return v1.getDepth() == v2.getDepth();
    }

    private static boolean ePf(StringGraphNode<?> v1, StringGraphNode<?> v2) {
        Set<String> v1PrincipalFunctors = principalFunctors(v1);
        Set<String> v2PrincipalFunctors = principalFunctors(v2);
        Set<String> intersection = new HashSet<>(v1PrincipalFunctors);
        intersection.retainAll(v2PrincipalFunctors);
        return intersection.size() == v1PrincipalFunctors.size() && v1PrincipalFunctors.size() == v2PrincipalFunctors.size();
    }

    private static Set<String> principalFunctors(StringGraphNode<?> v) {
        return v
                .getForwardChildren()
                .stream()
                .filter(_v -> _v instanceof ConcatStringGraphNode)
                .map(StringGraphNode::getLabel)
                .collect(Collectors.toSet());
    }

    private static List<StringGraphNode<?>> ca(StringGraphNode<?> v, StringGraphNode<?> v1, Set<List<StringGraphNode<?>>> C) {
        Optional<List<StringGraphNode<?>>> caOpt = C
                .stream()
                .filter(pair -> {
                    StringGraphNode<?> _va = pair.get(0);
                    StringGraphNode<?> _va1 = pair.get(1);
                    return _va.getForwardChildren().contains(v) && _va1.getForwardChildren().contains(v1);
                })
                .findFirst();
        return caOpt.orElseGet(ArrayList::new);
    }

    private static Set<List<StringGraphNode<?>>> CI(StringGraphNode<?> go, StringGraphNode<?> gn) {
        Set<List<StringGraphNode<?>>> CI = new HashSet<>();
        for (List<StringGraphNode<?>> pair : wideningTopologicalClashes(go, gn)) {
            List<StringGraphNode<?>> element = new ArrayList<>();
            StringGraphNode<?> vo = pair.get(0);
            StringGraphNode<?> vn = pair.get(1);
            Optional<StringGraphNode<?>> vaOpt = vn
                    .getAncestors()
                    .stream()
                    .filter(_va -> vn.isLessOrEqual(_va) && vo.getDepth() >= _va.getDepth())
                    .findFirst();
            if (vaOpt.isPresent()) {
                StringGraphNode<?> va = vaOpt.get();
                List<StringGraphNode<?>> ca = ca(vo, vn, correspondenceSet(go, gn));
                if (ca.size() == 2) {
                    StringGraphNode<?> v = ca.get(1);
                    element.add(v);
                    element.add(vn);
                    element.add(v);
                    element.add(va);
                    CI.add(element);
                }
            }
        }
        return CI;
    }

    private static Set<List<StringGraphNode<?>>> CR(StringGraphNode<?> go, StringGraphNode<?> gn) {
        Set<List<StringGraphNode<?>>> CR = new HashSet<>();
        for (List<StringGraphNode<?>> pair : wideningTopologicalClashes(go, gn)) {
            List<StringGraphNode<?>> element = new ArrayList<>();
            StringGraphNode<?> vo = pair.get(0);
            StringGraphNode<?> vn = pair.get(1);
            Optional<StringGraphNode<?>> vaOpt = vn
                    .getAncestors()
                    .stream()
                    .filter(_va -> !vn.isLessOrEqual(_va)
                            && principalFunctors(_va).containsAll(principalFunctors(vn))
                            && vo.getDepth() >= _va.getDepth())
                    .findFirst();
            if (vaOpt.isPresent()) {
                StringGraphNode<?> va = vaOpt.get();
                element.add(vn);
                element.add(va);
                CR.add(element);
            }
        }
        return CR;
    }
}
