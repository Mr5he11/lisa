package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;
import java.util.*;
import java.util.stream.Collectors;

public class OrStringGraphNode<C extends StringGraphNode<?,C,?, OrStringGraphNode<C,P>>, P extends StringGraphNode<?,P, OrStringGraphNode<C,P>,?>>
        extends StringGraphNode<Void, OrStringGraphNode<C,P>,C,P> {

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

        for (C n : getChildren()) {
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
}
