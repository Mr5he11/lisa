package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConcatStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.SimpleStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.StringGraphNode;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

public class StringGraphDomain extends BaseNonRelationalValueDomain<StringGraphDomain> {

	private static final StringGraphDomain TOP = new StringGraphDomain(SimpleStringGraphNode.MAX);
	private static final StringGraphDomain BOTTOM = new StringGraphDomain(SimpleStringGraphNode.MIN);
	
	private StringGraphNode root;
	
	public StringGraphDomain() {
		this(new SimpleStringGraphNode());
	}
	
	public StringGraphDomain(StringGraphNode root) {
		this.root = root;
	}
	
	@Override
	public String representation() {
		return this.root != null ? this.root.toString() : null;
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

			// can be a SimpleNode or a ConcatNode, depending on the length of value
			StringGraphNode node = StringGraphNode.create(value);
			return new StringGraphDomain(node);
		}
		return super.evalNonNullConstant(constant, pp);
	}

	@Override
	protected StringGraphDomain evalUnaryExpression(UnaryOperator operator, StringGraphDomain arg, ProgramPoint pp) {
		return super.evalUnaryExpression(operator, arg, pp);
	}

	@Override
	protected StringGraphDomain evalBinaryExpression(BinaryOperator operator, StringGraphDomain left,
			StringGraphDomain right, ProgramPoint pp) {
		
		if (BinaryOperator.STRING_CONCAT == operator) {
			
			ConcatStringGraphNode concatNode = new ConcatStringGraphNode();
			
			concatNode.addChild(left.root);
			concatNode.addChild(right.root);

			// TODO add backward-edges from left/right to concatNode?
			
			if (this.root == null) {
				this.root = concatNode;
			} else {
				
				// TODO merge the root with the created node

			}
			
		}
		return super.evalBinaryExpression(operator, left, right, pp);
	}

	@Override
	protected StringGraphDomain evalTernaryExpression(TernaryOperator operator, StringGraphDomain left,
			StringGraphDomain middle, StringGraphDomain right, ProgramPoint pp) {
		return super.evalTernaryExpression(operator, left, middle, right, pp);
	}
	
	@Override
	protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected StringGraphDomain wideningAux(StringGraphDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected boolean lessOrEqualAux(StringGraphDomain other) throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringGraphDomain other = (StringGraphDomain) obj;
		if (root == null) {
			if (other.root != null)
				return false;
		} else if (!root.equals(other.root))
			return false;
		return true;
	}    
}
