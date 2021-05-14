package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.*;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class StringGraphDomain extends BaseNonRelationalValueDomain<StringGraphDomain> {
	private final StringGraphNode<?> root;
	
	public StringGraphDomain() {
		this( new ConstStringGraphNode(ConstValues.EMPTY) );
	}
	
	public StringGraphDomain(StringGraphNode<?> root) {
		this.root = root;
	}
	
	@Override
	public String representation() {
		return this.root != null ? this.root.toString() : null;
	}

	@Override
	public StringGraphDomain top() {
		return new StringGraphDomain(new ConstStringGraphNode(ConstValues.MAX));
	}

	@Override
	public StringGraphDomain bottom() {
		return new StringGraphDomain(new ConstStringGraphNode(ConstValues.MIN));
	}

	@Override
	protected StringGraphDomain evalTypeConv(BinaryExpression conv, StringGraphDomain left, StringGraphDomain right) {
		return super.evalTypeConv(conv, left, right);
	}

	@Override
	protected StringGraphDomain evalTypeCast(BinaryExpression cast, StringGraphDomain left, StringGraphDomain right) {
		return super.evalTypeCast(cast, left, right);
	}

	@Override
	protected StringGraphDomain evalNullConstant(ProgramPoint pp) {
		return super.evalNullConstant(pp);
	}

	@Override
	protected StringGraphDomain evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof String) {
			String value = (String)constant.getValue();
			StringGraphNode<?> node = StringGraphNode.create(value);
			return new StringGraphDomain(node);
		}
		return bottom();
	}

	@Override
	protected StringGraphDomain evalUnaryExpression(UnaryOperator operator, StringGraphDomain arg, ProgramPoint pp) {
		return bottom();
	}

	@Override
	protected StringGraphDomain evalBinaryExpression(BinaryOperator operator, StringGraphDomain left,
			StringGraphDomain right, ProgramPoint pp) {
		
		if (BinaryOperator.STRING_CONCAT == operator) {

			StringGraphNode<?> concatNode = new ConcatStringGraphNode();
			concatNode.addForwardChild(left.root);
			concatNode.addForwardChild(right.root);

			// TODO normalize concatNode

			return new StringGraphDomain(concatNode);
			
		}

		if (BinaryOperator.STRING_CONTAINS == operator) {
			// 4.4.6
			// checking only for a single character
			if (right.root instanceof SimpleStringGraphNode) {
				Character c = ((SimpleStringGraphNode)right.root).getValueAsChar();

				// check if "true" condition of section 4.4.6 holds
				boolean containsCheckTrue = containsCheckTrueAux(left.root, c);
				
				if (!containsCheckTrue) {
					// checks if "false" condition of section 4.4.6 holds
					boolean containsCheckFalse = containsCheckFalseAux(left.root, c);

					if (!containsCheckFalse) {
						return top(); // TODO TopBoolean??
					} else {
						// TODO how to return true?
					}

				} else {
					// TODO how to return true?
				}
				
			}

			// TODO how to return false?

		}

		return bottom();
	}

	private boolean containsCheckTrueAux(StringGraphNode<?> node, Character c) {

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
				if (containsCheckTrueAux(child, c)) {
					return true;
				}
			}

			if (child instanceof ConcatStringGraphNode) {
				Integer k = ((ConcatStringGraphNode)child).getValue();
				for (int i=0; i<k; i++) {

					if (containsCheckTrueAux(child, c)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	// return true iff no node is 'c' or MAX
	private boolean containsCheckFalseAux(StringGraphNode<?> node, Character c) {

		if (node instanceof ConstStringGraphNode) {
			return !ConstValues.MAX.equals( ((ConstStringGraphNode)node).getValue() );
		}

		if (node instanceof SimpleStringGraphNode) {
			return !((SimpleStringGraphNode) node).getValueAsChar().equals(c);
		}

		// for all children must hold (no node is 'c' or MAX)
		for (StringGraphNode<?> child: node.getChildren()) {
			if (!containsCheckFalseAux(child, c)) {
				return false;
			}
		}

		// survived the check
		return true;
	}

	@Override
	protected StringGraphDomain evalTernaryExpression(TernaryOperator operator, StringGraphDomain left,
			StringGraphDomain middle, StringGraphDomain right, ProgramPoint pp) {

		if (TernaryOperator.STRING_SUBSTRING == operator) {
			// 4.4.6
			if ( left.root instanceof ConcatStringGraphNode ) {
				
			}

			return top();
		}

		return bottom();
	}
	
	@Override
	protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
		// 4.4.3
		StringGraphNode<?> orNode = new OrStringGraphNode();
		orNode.addForwardChild(this.root);
		orNode.addForwardChild(other.root);

		return new StringGraphDomain(orNode);
	}

	@Override
	protected StringGraphDomain wideningAux(StringGraphDomain other) throws SemanticException {
		// 4.4.4
		return null;
	}

	@Override
	protected boolean lessOrEqualAux(StringGraphDomain other) throws SemanticException {
		// 4.4.2
		return partialOrderAux(this.root, other.root, new HashSet<>());
	}
	
	private boolean partialOrderAux(StringGraphNode<?> n, StringGraphNode<?> m, Set<Map.Entry<StringGraphNode<?>, StringGraphNode<?>>> edges) {
		
		// case (1)
		if (edges.contains( StringGraphNode.createEdge(n, m))) {
			return true;
		}
		
		// case (2)
		if (m instanceof ConstStringGraphNode) {
			ConstValues constValue = ((ConstStringGraphNode)m).getValue();
			
			if (ConstValues.MAX == constValue) {
				return true;
			}
		}
		
		// case (3)
		if (n instanceof ConcatStringGraphNode && m instanceof ConcatStringGraphNode) {
			ConcatStringGraphNode concatN = (ConcatStringGraphNode)n;
			ConcatStringGraphNode concatM = (ConcatStringGraphNode)m;

			List<StringGraphNode<?>> childrenN = n.getChildren();
			List<StringGraphNode<?>> childrenM = m.getChildren();
			
			Integer k = concatN.getValue() != null ? concatN.getValue() : 0;
			
			if (k > 0 && k.compareTo(concatM.getValue()) == 0) {
				
				// add current edge to edgeSet
				edges.add(StringGraphNode.createEdge(n, m));

				// for each i in [0,k] must hold <=(n/i, m/i, edges+{m,n})
				for (int i=0; i<k; i++) {

					boolean isLessOrEqual = partialOrderAux(childrenN.get(i), childrenM.get(i), edges);
					
					if (!isLessOrEqual) return false;
				}
				
				return true;
			}
		}
		
		// case (4)
		if (n instanceof OrStringGraphNode && m instanceof OrStringGraphNode) {
			int k = n.getOutDegree();
			List<StringGraphNode<?>> children = n.getChildren();
			
			// add current edge to edgeSet
			edges.add(StringGraphNode.createEdge(n, m));

			// for each i in [0,k] must hold <=(n/i, m, edges+{m,n})
			for (int i=0; i<k; i++) {

				boolean isLessOrEqual = partialOrderAux(children.get(i), m, edges);
				
				if (!isLessOrEqual) return false;
			}

			return true;
			
		}
		
		// case (5)
		if (m instanceof OrStringGraphNode) {
			
			StringGraphNode<?> md = null;
			
			// look for a node 'md' in prnd(m) such that lb(n) == lb(md)
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StringGraphDomain that = (StringGraphDomain) o;
		return Objects.equals(root, that.root);
	}

	@Override
	public int hashCode() {
		return Objects.hash(root);
	}
}
