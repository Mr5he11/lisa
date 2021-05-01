package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConcatStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConstStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConstValues;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.OrStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.StringGraphNode;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class StringGraphDomain extends BaseNonRelationalValueDomain<StringGraphDomain> {
	private final StringGraphNode<?,?,?,?> root;
	
	public StringGraphDomain() {
		this( new ConstStringGraphNode<>(ConstValues.EMPTY) );
	}
	
	public StringGraphDomain(StringGraphNode<?,?,?,?> root) {
		this.root = root;
	}
	
	@Override
	public String representation() {
		return this.root != null ? this.root.toString() : null;
	}

	@Override
	public StringGraphDomain top() {
		return new StringGraphDomain(new ConstStringGraphNode<>(ConstValues.MAX));
	}

	@Override
	public StringGraphDomain bottom() {
		return new StringGraphDomain(new ConstStringGraphNode<>(ConstValues.MIN));
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
			StringGraphNode<?,?,?,?> node = StringGraphNode.create(value);
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

			StringGraphNode<?,?,?,?> concatNode = new ConcatStringGraphNode(left.root, right.root);

			// TODO normalize concatNode

			return new StringGraphDomain(concatNode);
			
		}

		if (BinaryOperator.STRING_CONTAINS == operator) {

			// TODO 4.4.6

		}

		return bottom();
	}

	@Override
	protected StringGraphDomain evalTernaryExpression(TernaryOperator operator, StringGraphDomain left,
			StringGraphDomain middle, StringGraphDomain right, ProgramPoint pp) {

		if (TernaryOperator.STRING_SUBSTRING == operator) {

			// TODO 4.4.6

		}

		return bottom();
	}
	
	@Override
	protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
		// 4.4.3
		StringGraphNode<?,?,?,?> orNode = new OrStringGraphNode(this.root, other.root);

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
	
	private <T extends StringGraphNode<?,?,?,?>, V extends StringGraphNode<?,?,?,?>> boolean 
		partialOrderAux(T n, V m, Set<Map.Entry<StringGraphNode<?,?,?,?>, StringGraphNode<?,?,?,?>>> edges) {
		
		// case (1)
		if (edges.contains( StringGraphNode.createEdge(n, m))) {
			return true;
		}
		
		// case (2)
		if (m instanceof ConstStringGraphNode) {
			ConstValues constValue = ((ConstStringGraphNode<?,?>)m).getValue();
			
			if (ConstValues.MAX == constValue) {
				return true;
			}
		}
		
		// case (3)
		if (n instanceof ConcatStringGraphNode && m instanceof ConcatStringGraphNode) {
			ConcatStringGraphNode<?,?> concatN = (ConcatStringGraphNode<?,?>)n;
			ConcatStringGraphNode<?,?> concatM = (ConcatStringGraphNode<?,?>)m;

			List<StringGraphNode<?,?,?,?>> childrenN = (List<StringGraphNode<?,?,?,?>>) n.getChildren();
			List<StringGraphNode<?,?,?,?>> childrenM = (List<StringGraphNode<?,?,?,?>>) m.getChildren();
			
			Integer k = concatN.getValue() != null ? concatN.getValue() : 0;
			
			if (k > 0 && k.compareTo(concatM.getValue()) == 0) {
				
				// add current edge to edgeSet
				Set<Entry<StringGraphNode<?, ?, ?, ?>, StringGraphNode<?, ?, ?, ?>>> copyOfEdges = Set.copyOf(edges);
				copyOfEdges.add(StringGraphNode.createEdge(n, m));
				
				// for each i in [0,k] must hold <=(n/i, m/i, edges+{m,n})
				// TODO include k?
				for (int i=0; i<k; i++) {
				
					// TODO n/i is n.getForwardNodes().get(i)?
					boolean isLessOrEqual = partialOrderAux(childrenN.get(i), childrenM.get(i), copyOfEdges);
					
					if (!isLessOrEqual) return false;
				}
				
				return true;
			}
		}
		
		// case (4)
		if (n instanceof OrStringGraphNode && m instanceof OrStringGraphNode) {
			int k = n.getOutDegree();
			List<StringGraphNode<?,?,?,?>> children = (List<StringGraphNode<?,?,?,?>>) n.getChildren();
			
			// add current edge to edgeSet
			Set<Entry<StringGraphNode<?, ?, ?, ?>, StringGraphNode<?, ?, ?, ?>>> copyOfEdges = Set.copyOf(edges);
			copyOfEdges.add(StringGraphNode.createEdge(n, m));
			
			// for each i in [0,k] must hold <=(n/i, m, edges+{m,n})
			// TODO include k?
			for (int i=0; i<k; i++) {
			
				// TODO n/i is n.getForwardNodes().get(i)?
				boolean isLessOrEqual = partialOrderAux(children.get(i), m, copyOfEdges);
				
				if (!isLessOrEqual) return false;
			}
			
		}
		
		// case (5)
		if (m instanceof OrStringGraphNode) {
			
			StringGraphNode<?,?,?,?> md = null;
			
			// look for a node 'md' in prnd(m) such that lb(n) == lb(md)
			for (StringGraphNode<?,?,?,?> prnd: m.getPrincipalNodes()) {
				if (prnd.getSelfClass().equals(n.getSelfClass())) {
					md = prnd; // found one
					break;
				}
			}
			
			
			if (md != null) {
				Set<Entry<StringGraphNode<?, ?, ?, ?>, StringGraphNode<?, ?, ?, ?>>> copyOfEdges = Set.copyOf(edges);
				copyOfEdges.add(StringGraphNode.createEdge(n, m));
				return partialOrderAux(n, md, copyOfEdges);
			}
			
		}
		
		// case (6)
		return ( n.getSelfClass().equals( m.getSelfClass() ));
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
