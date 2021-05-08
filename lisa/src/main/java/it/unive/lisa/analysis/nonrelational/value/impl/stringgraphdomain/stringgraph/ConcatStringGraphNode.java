package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConcatStringGraphNode extends StringGraphNode<Integer> {

    public ConcatStringGraphNode() {
    	super();
        this.value = 0;
    }

    public ConcatStringGraphNode(String value) {
        this();
        for (String s: value.split("")) {
            addForwardChild( new SimpleStringGraphNode(s) );
        }
    }

    @Override
    public <C extends StringGraphNode<?>> void addForwardChild(C child) {
        super.addForwardChild(child);
        this.value += 1;
    }

    @Override
    public <C extends StringGraphNode<?>> void addBackwardChild(C child) {
        super.addBackwardChild(child);
        this.value += 1;
    }

    @Override
    public <C extends StringGraphNode<?>> void removeChild(C child) {
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
    public String getLabel() {
        return "Concat/" + value;
    }

    @Override
    public List<String> getDenotation() {
        String s = "";
        List<String> result = new ArrayList<>();
        for (StringGraphNode<?> n : this.getChildren()) {
            if (n.isFinite()) {
                for (String str : n.getDenotation()) {
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
