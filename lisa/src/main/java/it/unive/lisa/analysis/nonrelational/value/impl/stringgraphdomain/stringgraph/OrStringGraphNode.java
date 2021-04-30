package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrStringGraphNode extends StringGraphNode<Void> {

    public OrStringGraphNode() {
        super();
        this.value = null;
    }

    public OrStringGraphNode(Collection<StringGraphNode> forwardNodes, Collection<StringGraphNode> backwardNodes) {
        this();
        for (StringGraphNode node : forwardNodes) {
            this.addForwardChild(node);
        }
        for (StringGraphNode node : backwardNodes) {
            this.addBackwardChild(node);
        }
    }
    
    @Override
	public String toString() {
        return "OR "
                + getChildren().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", " [", "]"));
	}

	@Override
    public List<String> getDenotation() {
        List<String> result = new ArrayList<>();

        List<StringGraphNode> union = Stream.concat(getForwardNodes().stream(), getBackwardNodes().stream())
                .distinct()
                .collect(Collectors.toList());

        for (StringGraphNode<?> n : union) {
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
