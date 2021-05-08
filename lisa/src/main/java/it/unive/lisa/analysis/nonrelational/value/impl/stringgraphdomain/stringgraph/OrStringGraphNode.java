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

    public OrStringGraphNode(Collection<C> forwardNodes, Collection<C> backwardNodes) {
        this();
        for (C node : forwardNodes) {
            this.addForwardChild(node);
        }
        for (C node : backwardNodes) {
            this.addBackwardChild(node);
        }
    }

    public OrStringGraphNode(C root1, C root2) {
        this();
        addForwardChild(root1);
        addForwardChild(root2);
    }

    @Override
	public String toString() {
        return "OR"
                + getChildren().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", " [", "]"));
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
    protected void compactAux() {
        if (this.getChildren().size() == 1) {
            // If Or node has only one child, replace such node with its child
           StringGraphNode.replaceNode(this, this.getChildren().get(0));
        }
        Iterator<StringGraphNode<?>> i = this.getChildren().iterator();
        boolean maxFound = false;
        while (i.hasNext() && !maxFound) {
            StringGraphNode<?> child = i.next();
            maxFound = child.getDenotation().get(0).compareTo(ConstValues.ALL_STRINGS.name()) == 0;
            if (maxFound) {
                // If one of the Or node children is an ALL_STRINGS, replace it with a MAX node
                ConstStringGraphNode node = new ConstStringGraphNode(ConstValues.MAX);
                StringGraphNode.replaceNode(this, node);
            } else if (child instanceof OrStringGraphNode) {
                // If one of the Or node children is a Or node itself, check that the latter has at least two parents,
                // otherwise remove it and add all of its children to the uniq parent node
                if (child.getForwardParents().size() + child.getBackwardParents().size() < 2) {
                    for (StringGraphNode<?> c : child.getForwardNodes()) {
                        this.addForwardChild(c);
                        child.removeChild(c);
                    }
                    this.removeChild(child);
                    child.removeParent(this);
                }
            }
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
	
	
}
