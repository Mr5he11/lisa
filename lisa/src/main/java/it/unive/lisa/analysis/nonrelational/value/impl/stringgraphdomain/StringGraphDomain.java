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
		this( new ConstStringGraphNode(ConstValues.MIN) );
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

			StringGraphNode<?> newNode = null;
			try {
				newNode = concatNode.normalize();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return new StringGraphDomain(newNode);
			
		}

		return top();
	}


	@Override
	protected StringGraphDomain evalTernaryExpression(TernaryOperator operator, StringGraphDomain left,
			StringGraphDomain middle, StringGraphDomain right, ProgramPoint pp) {
		return top();
	}
	
	@Override
	protected StringGraphDomain lubAux(StringGraphDomain other) throws SemanticException {
		// Section 4.4.3
		if (!(this.root.getDenotation().equals(other.root.getDenotation()))) {
			StringGraphNode<?> orNode = new OrStringGraphNode();
			orNode.addForwardChild(this.root);
			orNode.addForwardChild(other.root);
			StringGraphNode<?> result = orNode.normalize();
			return new StringGraphDomain(result);
		} else {
			return this;
		}
	}

	@Override
	public StringGraphDomain glbAux(StringGraphDomain other) {
		return super.glbAux(other);
	}

	@Override
	protected StringGraphDomain wideningAux(StringGraphDomain other) throws SemanticException {
		// 4.4.4

		StringGraphDomain go = this;
		StringGraphNode<?> orNode = new OrStringGraphNode();
		orNode.addForwardChild(this.root);
		orNode.addForwardChild(other.root);
		StringGraphNode<?> result = orNode.normalize();
		StringGraphDomain gn = new StringGraphDomain(result);

		// Topological clash between vo and vn:
		// 	(1): [vo]: OR node in gOld and [vn]: OR node in gNew where prlb(vo) <> prlb(vn)
		// 	(2): [vo]: OR node in gOld and [vn]: OR node in gNew where depth(vo) < depth(vn)

		StringGraphNode<?> vo = null, vn = null;

		List<StringGraphNode<?>> oldNodes = go.root.getForwardNodes();
		List<StringGraphNode<?>> newNodes = gn.root.getForwardNodes();

		for(StringGraphNode<?> oldNode: oldNodes) {
			boolean found = false;
			for(StringGraphNode<?> newNode: newNodes) {

				if (oldNode instanceof OrStringGraphNode && newNode instanceof OrStringGraphNode) {
					// (1) : prlb(vo) <> prlb(vn)
					Set<String> oldPrlb = oldNode.getPrincipalLabels();
					Set<String> newPrlb = newNode.getPrincipalLabels();

					if (oldPrlb == null && newPrlb != null) { found = true; }
					if (oldPrlb != null && newPrlb == null) { found = true; }
					if (oldPrlb != null && !oldPrlb.equals(newPrlb)) { found = true; }

					if (found) {
						vo = oldNode;
						vn = newNode;
						break;
					}
				}
			}

			if (found) {
				break;
			}
		}


		// If one of the previous is true, then search for an ancestor [va] of [vn] such that prlb(vn) is INCLUDED in prlb(va)
		// else do nothing
		if (vo == null || vn == null) {
			return top();
		}

		StringGraphNode<?> va = null;
		Set<String> nPrlb = vn.getPrincipalLabels();

		StringGraphNode<?> parent = vn.getForwardParent();
		while(va == null && parent != null) {
			Set<String> parentPrlb = parent.getPrincipalLabels();
			if (parentPrlb != null && parentPrlb.containsAll(nPrlb)) {
				// found ancestor!
				va = parent;
			}

			parent = parent.getForwardParent();
		}

		if (va == null) {
			return top();
		}
		// If ancestor [va] is found and <=(vn, va) then a cycle can be introduced.
		// else replace va with a OR node with [va, vn] as children. Then re-apply widening.
		if (StringGraphNode.isLessOrEqual(vn, va)) {
			vn.addBackwardChild(va);
			return gn;
		} else {
			OrStringGraphNode or = new OrStringGraphNode();

			StringGraphNode<?> vaOriginalParent = va.getForwardParent();
			vaOriginalParent.removeChild(va);
			vaOriginalParent.addForwardChild(or);

			vn.getForwardParent().removeChild(vn);

			or.addForwardChild(va);
			or.addForwardChild(vn);

			return this.wideningAux(gn); // ???
			//return other.wideningAux(gn); // ???
		}
	}

	@Override
	protected boolean lessOrEqualAux(StringGraphDomain other) throws SemanticException {
		return StringGraphNode.isLessOrEqual(this.root, other.root);
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
				if (StringGraphNode.containsCheckTrueAux(left.root, c)) {
					return SemanticDomain.Satisfiability.SATISFIED;
				}

				// checks if "false" condition of Section 4.4.6 - Table VII holds
				if (StringGraphNode.containsCheckFalseAux(left.root, c)) {
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
		return Objects.hash(root);
	}
}
