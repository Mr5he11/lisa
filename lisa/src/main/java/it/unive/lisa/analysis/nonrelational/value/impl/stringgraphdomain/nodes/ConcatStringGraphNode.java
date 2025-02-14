package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.nodes.ConstStringGraphNode.ConstValues;

import java.util.ArrayList;
import java.util.List;

public class ConcatStringGraphNode extends StringGraphNode<Integer> {

    public int desiredNumberOfChildren = 0;

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
    public <C extends StringGraphNode<?>> void addForwardChild(int index, C child) {
        super.addForwardChild(index, child);
        this.value += 1;
    }

    @Override
    public <C extends StringGraphNode<?>> void addBackwardChild(C child) {
        super.addBackwardChild(child);
        this.value += 1;
    }

    @Override
    public <C extends StringGraphNode<?>> void addBackwardChild(int index, C child) {
        super.addBackwardChild(index, child);
        this.value += 1;
    }

    @Override
    public <C extends StringGraphNode<?>> void removeChild(C child) {
        if (this.getChildren().contains(child)) {
            super.removeChild(child);
            this.value -= 1;
        }
    }

    @Override
    public String getLabel() {
        return "Concat/" + value;
    }

    @Override
    public List<String> getDenotation() {
        String s = "";
        List<String> result = new ArrayList<>();
        for (StringGraphNode<?> n : this.getForwardChildren()) {
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
