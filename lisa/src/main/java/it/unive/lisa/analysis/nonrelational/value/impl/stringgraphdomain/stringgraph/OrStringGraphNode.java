package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;
import java.util.*;

public class OrStringGraphNode extends StringGraphNode<Void> {

    public OrStringGraphNode() {
        this.value = null;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    public OrStringGraphNode(Collection<StringGraphNode> parents, Collection<StringGraphNode> children) {
        this();
        this.parents.addAll(parents);
        this.parents.addAll(children);
    }
    
    @Override
	public String toString() {
		return "OR ["+ (children != null ? children.toString() : "") +"]";
	}

	@Override
    public List<String> getDenotation() {
        List<String> result = new ArrayList<>();
        for (StringGraphNode n : this.getChildren()) {
            for (Object el : n.getDenotation()) {
                String str = (String) el;
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
