package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcatStringGraphNode extends StringGraphNode<Integer> {

    public ConcatStringGraphNode() {
        super();
        this.value = 0;
    }

    public ConcatStringGraphNode(String value) {
        super();
        for (String s: value.split("")) {
            addForwardChild(new SimpleStringGraphNode(s));
        }
    }

    @Override
    protected void addForwardChild(StringGraphNode child) {
        super.addForwardChild(child);
        this.value += 1;
    }

    @Override
    protected void addBackwardChild(StringGraphNode child) {
        super.addBackwardChild(child);
        this.value += 1;
    }

    @Override
    protected void removeChild(StringGraphNode child) {
        super.removeChild(child);
        this.value -= 1;
    }

    @Override
	public String toString() {
		return "Concat/" + value
                + getChildren().stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", ", " [", "]"));
	}

	@Override
    public List<String> getDenotation() {
        String s = "";
        List<String> result = new ArrayList<>();
        for (StringGraphNode n : this.getChildren()) {
            if (n.isFinite(n)) {
                for (Object el : n.getDenotation()) {
                    String str = (String) el;
                    // Concat happens only if none of the child nodes is TOP, otherwise result is all possible strings
                    if (ConstValues.ALL_STRINGS.name().compareTo(s) != 0 && ConstValues.ALL_STRINGS.name().compareTo(str) != 0 )
                        s = s.concat(str);
                    else
                        s = ConstValues.ALL_STRINGS.name();
                }
            }
        }
        result.add(s);
        return result;
    }
    
}
