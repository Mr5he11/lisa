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
		this( new ConstStringGraphNode(ConstValues.MAX) );
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
			String value = (String)constant.getValue();
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
			return top();
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
			return top();
		}
		// If ancestor [va] is found and <=(vn, va) then a cycle can be introduced.
		// else replace [va] with a OR node with [va, vn] as children. Then re-apply widening.
		if (vn.isLessOrEqual(va)) {
			// introduce a cycle in the graph!
			vn.addBackwardChild(va);
			return gn;
		} else {
			OrStringGraphNode or = new OrStringGraphNode();

			// remove va from parent and add or as child
			StringGraphNode<?> vaOriginalParent = va.getForwardParent();
			vaOriginalParent.removeChild(va);
			vaOriginalParent.addForwardChild(or);

			// remove vn from parent (since now vn is child of or)
			vn.getForwardParent().removeChild(vn);

			// add va and vn as children
			or.addForwardChild(va);
			or.addForwardChild(vn);

			return widening(gn);
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
