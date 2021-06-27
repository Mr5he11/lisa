package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.*;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.ConstStringGraphNode.ConstValues;
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
            return new StringGraphDomain(StringGraphNode.create(value));
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
            return new StringGraphDomain(SGNUtils.normalize(concatNode));
        }
        return top();
    }

    @Override
    protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
        // Section 4.4.3
        StringGraphNode<?> orNode = new OrStringGraphNode();
        orNode.addForwardChild(this.root);
        orNode.addForwardChild(other.root);
        StringGraphNode<?> result = SGNUtils.normalize(orNode);
        return new StringGraphDomain(result);
    }

    /**
     * Intersection algorithm taken from the appendix of <a href="https://doi.org/10.1016/0743-1066(92)90032-X">
     * G. Janssens, M. Bruynooghe,
     * Deriving descriptions of possible values of program variables by means of abstract interpretation,
     * The Journal of Logic Programming,
     * Volume 13, Issues 2–3,
     * 1992,</a>.
     *
     * @param other the abstract domain element which root should be intersected with this.root
     * @return an abstract domain element representing the intersection (and so, the glb) between this.root and other.root
     */
    @Override
    public StringGraphDomain glbAux(StringGraphDomain other) {
        /* INITIALIZATION */
        // Since the expected denotation should be an intersection (hence a list), the node must be an OR node
        StringGraphNode<?> l0 = new OrStringGraphNode();
        Set<StringGraphNode<?>> S_sn = new LinkedHashSet<>();
        Set<StringGraphNode<?>> S_ul = new LinkedHashSet<>();
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

                for (int idx = 0; idx < m.getForwardChildren().size(); idx++) {
                    StringGraphNode<?> mi = m.getForwardChildren().get(idx);
                    StringGraphNode<?> li = new OrStringGraphNode();
                    li.is().add(mi);
                    li.is().addAll(ris_no_m);
                    S_ul.add(li);
                    l.addForwardChild(li);
                }
                continue;
            }

            // RULE 4
            Optional<StringGraphNode<?>> opt = l.is().stream().filter(x -> x instanceof ConcatStringGraphNode || x instanceof SimpleStringGraphNode).findAny();
            if (opt.isPresent()) {
                StringGraphNode<?> n = opt.get();

                boolean labelFound = l.ris().stream().filter(x -> !x.equals(n)).allMatch(m ->
                        m.getPrincipalNodes().stream().anyMatch(md -> md.getLabel().equals(n.getLabel()))
                );

                if (labelFound) {
                    SGNUtils.replace(l, n);
                    S_ul.remove(l);
                    S_sn.add(n);

                    if (n instanceof ConcatStringGraphNode) {

                        final Set<StringGraphNode<?>> S_md = new HashSet<>();
                        l.ris().stream().filter(x -> !x.equals(n)).forEach(m ->
                            m.getPrincipalNodes().stream().filter(md -> md.getLabel().equals(n.getLabel())).findAny().ifPresent(S_md::add)
                        );

                        for (int idx = 0; idx < n.getForwardChildren().size(); idx++) {
                            StringGraphNode<?> ni = n.getForwardChildren().get(idx);
                            StringGraphNode<?> li = new OrStringGraphNode();
                            li.is().add(ni);
                            l.addForwardChild(li);
                            int finalIdx = idx;
                            S_md.forEach(m ->  li.is().add(m.getForwardChildren().get(finalIdx)));
                            S_ul.add(li);
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
        OrStringGraphNode tmp = new OrStringGraphNode();
        tmp.addForwardChild(this.root);
        tmp.addForwardChild(other.root);
        StringGraphNode<?> gn = SGNUtils.normalize(tmp);
        StringGraphNode<?> widened = SGNUtils.normalize(SGNUtils.widening(this.root, gn));
        return new StringGraphDomain(widened);
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
