package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.*;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.*;

import java.util.Objects;

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
		return false;
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
