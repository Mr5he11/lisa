package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Iterator;
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
                + getForwardNodes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", " [", "]"))
                + getBackwardNodes().stream()
                .map(StringGraphNode::getLabel)
                .collect(Collectors.joining(", ", " {", "}"));
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

    @Override
    public void normalizeAux() {
        if (this.getChildren().size() == 1) {
            // Rule 1
            StringGraphNode.replaceNode(this, this.getChildren().get(0));
        } else {
            boolean allMax = true;
            int index = 0;
            Iterator<StringGraphNode<?>> childIterator = this.getChildren().iterator();
            while (allMax && childIterator.hasNext()) {
                StringGraphNode<?> child = childIterator.next();
                if (!(child instanceof ConstStringGraphNode && child.getValue() == ConstValues.MAX.name())) {
                    allMax = false;
                    // Rule 3 and Rule 4
                    child.normalizeAux();
                    if (child instanceof ConcatStringGraphNode && child.getInDegree() < 2) {
                        for (StringGraphNode<?> c : child.getChildren()) {
                            this.addForwardChild(index, c);
                            child.removeChild(c);
                        }
                        this.removeChild(child);
                    }
                }
                index += 1;
            }
            if (allMax) {
                // Rule 2
                StringGraphNode.replaceNode(this, new ConstStringGraphNode(ConstValues.MAX));
            }
        }
    }
}
