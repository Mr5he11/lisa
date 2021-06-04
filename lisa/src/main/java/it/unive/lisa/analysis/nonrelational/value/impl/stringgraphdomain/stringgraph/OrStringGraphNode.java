package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class OrStringGraphNode extends StringGraphNode<Void> {

    public OrStringGraphNode() {
        super();
        this.value = null;
    }

    @Override
	public String toString() {
        return "OR"
                + getForwardNodes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", " [", "]"))
                + getBackwardNodes().stream()
                .map(StringGraphNode::getLabel)
                .collect(Collectors.joining(", ", " {", "}"));
	}

	@Override
    public List<String> getDenotation() {
        List<String> result = new ArrayList<>();

        for (StringGraphNode<?> n : getChildren()) {
            for (String str : n.getDenotation()) {
                if (
                    (result.size() == 1 && ConstValues.ALL_STRINGS.name().compareTo(result.get(0)) == 0)
                    || ConstValues.ALL_STRINGS.name().compareTo(str) == 0
                ) {
                    result = new ArrayList<>();
                    result.add(ConstValues.ALL_STRINGS.name());
                } else
                    result.add(str);
            }
        }
        return result;
    }

    @Override
    protected StringGraphNode<?> normalizeAux() {
        if (this.getChildren().size() == 1) {
            // If Or node has only one child, replace such node with its child
           return this.getChildren().get(0);
        } else {
            List<StringGraphNode<?>> children = new ArrayList<>(this.getForwardNodes());
            for (StringGraphNode<?> child : children) {
                if (child.getDenotation().get(0).compareTo(ConstValues.ALL_STRINGS.name()) == 0) {
                    // If one of the Or node children is an ALL_STRINGS, replace it with a MAX node
                    return new ConstStringGraphNode(ConstValues.MAX);
                } else if (child instanceof OrStringGraphNode && child.getInDegree() < 2) {
                    // If one of the Or node children is a Or node itself, check that the latter has at least two parents,
                    // otherwise remove it and add all of its children to the uniq parent node
                    List<StringGraphNode<?>> subChildren = new ArrayList<>(child.getForwardNodes());
                    for (StringGraphNode<?> c : subChildren) {
                        this.addForwardChild(c);
                    }
                    this.removeChild(child);
                }
            }
            return this;
        }
    }

	@Override
	public List<StringGraphNode<?>> getPrincipalNodes() {
		List<StringGraphNode<?>> prnds = new ArrayList<>();
		
		List<StringGraphNode<?>> children = this.getChildren();
		for (int i=0; i<getOutDegree(); i++) {
			
			List<StringGraphNode<?>> childPrnds = children.get(i).getPrincipalNodes();
			if (childPrnds != null) {
				prnds.addAll(childPrnds);
			}
		}
		return prnds;
	}

    @Override
    public String getLabel() {
        return "OR";
    }
}
