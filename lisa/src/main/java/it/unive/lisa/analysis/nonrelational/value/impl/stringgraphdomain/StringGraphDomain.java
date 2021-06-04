package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.*;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.*;

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
		return this.root.toString();
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
		return top();
	}

	@Override
	protected StringGraphDomain evalUnaryExpression(UnaryOperator operator, StringGraphDomain arg, ProgramPoint pp) {
		return top();
	}

	@Override
	protected StringGraphDomain evalBinaryExpression(BinaryOperator operator, StringGraphDomain left,
			StringGraphDomain right, ProgramPoint pp) {
		
		if (BinaryOperator.STRING_CONCAT == operator) {

			StringGraphNode<?> concatNode = new ConcatStringGraphNode();
			concatNode.addForwardChild(left.root);
			concatNode.addForwardChild(right.root);

			StringGraphNode<?> newNode = null;
			try {
				newNode = StringGraphNode.deepClone(concatNode);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (newNode != null)
				newNode.normalize();

			return new StringGraphDomain(newNode);
			
		}


		return top();
	}


	@Override
	protected StringGraphDomain evalTernaryExpression(TernaryOperator operator, StringGraphDomain left,
			StringGraphDomain middle, StringGraphDomain right, ProgramPoint pp) {
/*
		if (TernaryOperator.STRING_SUBSTRING == operator) {
			return top();
		}

		if (TernaryOperator.STRING_REPLACE == operator) {
			return top();
		}
*/
		return top();
	}
	
	@Override
	protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
		// Section 4.4.3
		StringGraphNode<?> orNode = new OrStringGraphNode();
		StringGraphNode<?> result = new OrStringGraphNode();
		orNode.addForwardChild(this.root);
		orNode.addForwardChild(other.root);
		try {
			result = StringGraphNode.deepClone(orNode);
		} catch (Exception e) {
			e.printStackTrace();
		}
		result.normalize();
		return new StringGraphDomain(result);
	}

	@Override
	protected StringGraphDomain wideningAux(StringGraphDomain other) throws SemanticException {
		// 4.4.4
		return null;
	}

	@Override
	protected boolean lessOrEqualAux(StringGraphDomain other) throws SemanticException {
		return partialOrderAux(this.root, other.root, new HashSet<>());
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

				// for each i in [0,k] must hold: <=(n/i, m/i, edges+{m,n})
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

			// for each i in [0,k] must hold: <=(n/i, m, edges+{m,n})
			for (int i=0; i<k; i++) {

				boolean isLessOrEqual = partialOrderAux(children.get(i), m, edges);
				
				if (!isLessOrEqual) return false;
			}

			return true;
			
		}
		
		// case (5)
		if (m instanceof OrStringGraphNode) {
			
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

	@Override
	protected SemanticDomain.Satisfiability satisfiesBinaryExpression(BinaryOperator operator, StringGraphDomain left, StringGraphDomain right, ProgramPoint pp) {

		if (BinaryOperator.STRING_CONTAINS == operator) {
			// 4.4.6
			// checking only for a single character
			SimpleStringGraphNode simpleGraphNode = StringGraphNode.getSingleCharacterString(right.root);
			if (simpleGraphNode != null) {
				Character c = simpleGraphNode.getValueAsChar();

				// check if "true" condition of Section 4.4.6 - Table VII holds
				if (containsCheckTrueAux(left.root, c)) {
					return SemanticDomain.Satisfiability.SATISFIED;
				}

				// checks if "false" condition of Section 4.4.6 - Table VII holds
				if (containsCheckFalseAux(left.root, c)) {
					return SemanticDomain.Satisfiability.NOT_SATISFIED;
				}

				return SemanticDomain.Satisfiability.UNKNOWN;
			}
		}


		return SemanticDomain.Satisfiability.UNKNOWN;
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

	/**
	 * Auxiliary function for implementing Contains.
	 * Following Section 4.4.6 - Table VII, this recursive algorithm checks whether there is no node which is labelled with the character 'c' or the constant MAX
	 *
	 * @param node the root of the graph tree of interest
	 * @param c the character which has NOT to be contained in the graph tree with root node
	 * @return true iff there is no node labelled 'c' or MAX
	 */
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
