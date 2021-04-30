package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.List;

public class ConcatStringGraphNode<C extends StringGraphNode<?,C,?, ConcatStringGraphNode<C,P>>, P extends StringGraphNode<?,P, ConcatStringGraphNode<C,P>,?>>
        extends StringGraphNode<Integer, ConcatStringGraphNode<C,P>,C,P> {

    public ConcatStringGraphNode() {
        super();
        this.value = 0;
    }

    public ConcatStringGraphNode(String value) {
        this();
        for (String s: value.split("")) {
            addForwardChild( (C) new SimpleStringGraphNode(s) );
        }
    }

    public ConcatStringGraphNode(C root1, C root2) {
        this();
        addForwardChild(root1);
        addForwardChild(root2);
    }

    @Override
    public void addForwardChild(C child) {
        super.addForwardChild(child);
        this.value += 1;
    }

    @Override
    public void addBackwardChild(C child) {
        super.addBackwardChild(child);
        this.value += 1;
    }

    @Override
    public void removeChild(C child) {
        super.removeChild(child);
        this.value -= 1;
    }

    @Override
	public String toString() {
		return "Concat/" + value;
	}

	@Override
    public List<String> getDenotation() {
        String s = "";
        List<String> result = new ArrayList<>();
        for (C n : this.getChildren()) {
            if (n.isFinite(n)) {
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
