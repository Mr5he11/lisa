package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.*;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConstStringGraphNode.ConstValues;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;

import java.util.*;

public class StringGraphDomain extends BaseNonRelationalValueDomain<StringGraphDomain> {
    private final StringGraphNode<?> root;

    private final static StringGraphDomain TOP = new StringGraphDomain(new ConstStringGraphNode(ConstValues.MAX));
    private final static StringGraphDomain BOTTOM = new StringGraphDomain(new ConstStringGraphNode(ConstValues.MIN));

    public StringGraphDomain() {
        this(new ConstStringGraphNode(ConstValues.MAX));
    }

    public StringGraphDomain(StringGraphNode<?> root) {
        this.root = root;
    }

    @Override
    public String representation() {
        return this.root.toString();
    }

    @Override
    public StringGraphDomain top() {
        return TOP;
    }

    @Override
    public StringGraphDomain bottom() {
        return BOTTOM;
    }

    @Override
    protected StringGraphDomain evalNonNullConstant(Constant constant, ProgramPoint pp) {
        if (constant.getValue() instanceof String) {
            String value = (String) constant.getValue();
            StringGraphNode<?> node = StringGraphNode.create(value);
            return new StringGraphDomain(node);
        }
        return top();
    }

    @Override
    protected StringGraphDomain evalBinaryExpression(BinaryOperator operator, StringGraphDomain left,
                                                     StringGraphDomain right, ProgramPoint pp) {
        if (BinaryOperator.STRING_CONCAT == operator) {

            StringGraphNode<?> concatNode = new ConcatStringGraphNode();
            concatNode.addForwardChild(left.root);
            concatNode.addForwardChild(right.root);

            StringGraphNode<?> newNode = SGNUtils.compact(concatNode);
            return new StringGraphDomain(newNode);
        }

        return top();
    }

    @Override
    protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
        // Section 4.4.3
        StringGraphNode<?> orNode = new OrStringGraphNode();
        orNode.addForwardChild(this.root);
        orNode.addForwardChild(other.root);
        StringGraphNode<?> result = SGNUtils.compact(orNode);
        return new StringGraphDomain(result);
    }

    /**
     * APPENDIX
     * of "DERIVING DESCRIPTIONS OF POSSIBLE VALUES OF PROGRAM VARIABLES BY MEANS OF ABSTRACT INTERPRETATION"
     * by G. JANSSENS AND M. BRUYNOOGHE
     */
    @Override
    public StringGraphDomain glbAux(StringGraphDomain other) {
        // Since the expected denotation should be an intersection (hence a list), the node must be an OR node
        StringGraphNode<?> l0 = new OrStringGraphNode();

        /* INITIALIZATION */
        Set<StringGraphNode<?>> S_sn = new HashSet<>();
        Set<StringGraphNode<?>> S_ul = new HashSet<>();
        S_ul.add(l0);

        l0.is().add(this.root);
        l0.is().add(other.root);

        /* REPEAT-UNTIL */
        do {
            // select random l from S_ul
            StringGraphNode<?> l = S_ul.iterator().next();

            // RULE 1
            if (!S_sn.isEmpty()) {
                StringGraphNode<?> lp = l.getForwardParent();

                // check if exists an OR Node from a generic lg in S_sn to l.parent
                StringGraphNode<?> lg = null;
                for (StringGraphNode<?> _lg : S_sn) {
                    List<StringGraphNode<?>> path = SGNUtils.getForwardPath(_lg, lp);
                    if (path.stream().anyMatch(p -> !(p instanceof OrStringGraphNode))) {
                        lg = _lg;
                        break;

                    }
                }

                if (lg != null) {
                    S_ul.remove(l);
                    lp.addBackwardChild(lg);
                    lp.removeChild(l);
                    continue;
                }
            }

            // RULE 2
            if (l.ris().isEmpty()) {
                ConstStringGraphNode maxNode = new ConstStringGraphNode(ConstValues.MAX);
                SGNUtils.replace(l, maxNode);
                S_ul.remove(l);
                S_sn.add(maxNode);
                continue;
            }

            // RULE 3
            if (l.ris().stream().allMatch(n -> n instanceof OrStringGraphNode)) {
                OrStringGraphNode orNode = new OrStringGraphNode();
                SGNUtils.replace(l, orNode);
                S_ul.remove(l);
                S_sn.add(orNode);

                OrStringGraphNode m = (OrStringGraphNode) l.ris().iterator().next();
                Set<StringGraphNode<?>> ris_no_m = new HashSet<>(l.ris());
                ris_no_m.remove(m);

                for (int idx = 0; idx < m.getForwardNodes().size(); idx++) {
                    StringGraphNode<?> mi = m.getForwardNodes().get(idx);
                    StringGraphNode<?> li = new OrStringGraphNode();
                    li.is().add(mi);
                    li.is().addAll(ris_no_m);
                    S_ul.add(li);

                    /*StringGraphNode<?> childToRemove = orNode.getForwardNodes().get(idx);
                    orNode.addForwardChild(idx, li);
                    orNode.removeChild(childToRemove);*/
                }

                continue;

            }

            // RULE 4
            Optional<StringGraphNode<?>> opt = l.is().stream().filter(x -> x instanceof ConcatStringGraphNode || x instanceof SimpleStringGraphNode).findAny();
            if (opt.isPresent()) {
                StringGraphNode<?> n = opt.get();

                boolean boh = l.ris().stream().filter(x -> !x.equals(n)).allMatch(m -> {
                    Optional<StringGraphNode<?>> md_opt = m.getPrincipalNodes().stream().filter(md -> md.getLabel().equals(n.getLabel())).findAny();
                    return md_opt.isPresent();
                });

                if (boh) {
                    SGNUtils.replace(l, n);
                    S_ul.remove(l);
                    S_sn.add(n);


                    if (n instanceof ConcatStringGraphNode) {

                        final Set<StringGraphNode<?>> S_md = new HashSet<>();
                        l.ris().stream().filter(x -> !x.equals(n)).forEach(m -> {
                            Optional<StringGraphNode<?>> md_opt = m.getPrincipalNodes().stream().filter(md -> md.getLabel().equals(n.getLabel())).findAny();
                            md_opt.ifPresent(S_md::add);
                        });


                        for (int idx = 0; idx < n.getForwardNodes().size(); idx++) {
                            StringGraphNode<?> ni = n.getForwardNodes().get(idx);
                            StringGraphNode<?> li = new OrStringGraphNode();
                            li.is().add(ni);

                            int finalIdx = idx;
                            S_md.forEach(m ->  li.is().add(m.getForwardNodes().get(finalIdx)));

                            S_ul.add(li);

                            //SGNUtils.replace(ni, li);
                        }

                    }

                } else {
                    ConstStringGraphNode minNode = new ConstStringGraphNode(ConstValues.MIN);
                    SGNUtils.replace(l, minNode);
                    S_ul.remove(l);
                    S_sn.add(minNode);
                }

            }
        } while (!S_ul.isEmpty());

        return new StringGraphDomain(SGNUtils.compact(l0));
    }

    @Override
    protected StringGraphDomain wideningAux(StringGraphDomain other) throws SemanticException {
        // 4.4.4
        StringGraphDomain go = this;
        StringGraphNode<?> orNode = new OrStringGraphNode();
        orNode.addForwardChild(this.root);
        orNode.addForwardChild(other.root);
        StringGraphNode<?> result = SGNUtils.compact(orNode);
        StringGraphDomain gn = new StringGraphDomain(result);

        // Topological clash between vo and vn:
        // 	(1): [vo]: OR node in gOld and [vn]: OR node in gNew where prlb(vo) <> prlb(vn)
        // 	(2): [vo]: OR node in gOld and [vn]: OR node in gNew where depth(vo) < depth(vn)

        StringGraphNode<?> vn = SGNUtils.<StringGraphNode<?>>checkConditionInGraphs(go.root, gn.root, (oldNode, newNode) -> {
            StringGraphNode<?> returnValue = null;
            if (oldNode instanceof OrStringGraphNode && newNode instanceof OrStringGraphNode) {
                // (1) : prlb(vo) <> prlb(vn)
                Set<String> oldPrlb = oldNode.getPrincipalLabels();
                Set<String> newPrlb = newNode.getPrincipalLabels();
                if ((oldPrlb == null && newPrlb != null) || (oldPrlb != null && newPrlb == null) || (oldPrlb != null && !oldPrlb.equals(newPrlb)))
                    returnValue = newNode;

                // (2) : depth(vo) < depth(vn)
                Integer oldDistance = oldNode.getDistance(go.root);
                Integer newDistance = newNode.getDistance(gn.root);
                if (oldDistance != null && newDistance != null) {
                    if (oldDistance < newDistance) {
                        returnValue = newNode;
                    }
                }
            }
            return returnValue;
        });


        // If one of the previous is true, then search for an ancestor [va] of [vn] such that prlb(vn) is INCLUDED in prlb(va)
        // else do nothing
        if (vn == null) {
            return gn;
        }

        StringGraphNode<?> va = null;
        Set<String> nPrlb = vn.getPrincipalLabels();

        if (nPrlb != null) {
            StringGraphNode<?> parent = vn.getForwardParent();
            while (va == null && parent != null) {
                Set<String> parentPrlb = parent.getPrincipalLabels();
                if (parentPrlb != null && parentPrlb.containsAll(nPrlb)) {
                    // found ancestor!
                    va = parent;
                }
                parent = parent.getForwardParent();
            }
        }

        if (va == null) {
            return gn;
        }
        // If ancestor [va] is found and <=(vn, va) then a cycle can be introduced.
        // else replace [va] with a OR node with [va, vn] as children. Then re-apply widening.
        if (vn.isLessOrEqual(va)) {
            // introduce a cycle in the graph!
            StringGraphNode<?> vnParent = vn.getForwardParent();
            vnParent.addBackwardChild(va);
            vnParent.removeChild(vn);
            StringGraphNode<?> normalized = SGNUtils.compact(result);
            return new StringGraphDomain(normalized);
        } else {
            OrStringGraphNode or = new OrStringGraphNode();

            // remove va from parent and add or as child
            if (!va.isRoot()) {
                StringGraphNode<?> vaParent = va.getForwardParent();
                vaParent.removeChild(va);
                vaParent.addForwardChild(or);
            }

/*
            StringGraphNode<?> currentNode = vn;
            StringGraphNode<?> parent = currentNode.getForwardParent();
            while (parent != null && !parent.equals(va)) {

                parent.removeChild(currentNode);
                currentNode = parent;
                parent = currentNode.getForwardParent();

            }
*/
            // remove vn from parent (since now vn is child of or)
            vn.getForwardParent().removeChild(vn);

            // add va and vn as children
            or.addForwardChild(va);
            or.addForwardChild(vn);

            StringGraphNode<?> normalized = SGNUtils.compact(or);
            return widening(new StringGraphDomain(normalized));
        }
    }

    @Override
    protected boolean lessOrEqualAux(StringGraphDomain other) throws SemanticException {
        return this.root.isLessOrEqual(other.root);
    }

    @Override
    protected SemanticDomain.Satisfiability satisfiesBinaryExpression(BinaryOperator operator, StringGraphDomain left, StringGraphDomain right, ProgramPoint pp) {
        if (BinaryOperator.STRING_CONTAINS == operator) {
            // 4.4.6
            // checking only for a single character
            SimpleStringGraphNode simpleGraphNode = SGNUtils.getSingleCharacterString(right.root);
            if (simpleGraphNode != null) {
                Character c = simpleGraphNode.getValueAsChar();

                // check if "true" condition of Section 4.4.6 - Table VII holds
                if (SGNUtils.strContainsAux_checkTrue(left.root, c)) {
                    return SemanticDomain.Satisfiability.SATISFIED;
                }

                // checks if "false" condition of Section 4.4.6 - Table VII holds
                if (SGNUtils.strContainsAux_checkFalse(left.root, c)) {
                    return SemanticDomain.Satisfiability.NOT_SATISFIED;
                }

                return SemanticDomain.Satisfiability.UNKNOWN;
            }
        }

        return SemanticDomain.Satisfiability.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringGraphDomain that = (StringGraphDomain) o;
        return Objects.equals(root, that.root);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(root);
    }
}
