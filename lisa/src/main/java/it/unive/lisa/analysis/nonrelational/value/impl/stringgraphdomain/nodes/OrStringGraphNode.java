package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.ConstStringGraphNode.ConstValues;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class OrStringGraphNode extends StringGraphNode<Void> {

    public OrStringGraphNode() {
        super();
        this.value = null;
    }

	@Override
    public List<String> getDenotation() {
        List<String> result = new ArrayList<>();

        for (StringGraphNode<?> n : getForwardChildren()) {
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
	public Set<StringGraphNode<?>> getPrincipalNodes() {
        return  getForwardChildren().stream()
                .flatMap(s->s.getPrincipalNodes().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public String getLabel() {
        return "OR";
    }
}
