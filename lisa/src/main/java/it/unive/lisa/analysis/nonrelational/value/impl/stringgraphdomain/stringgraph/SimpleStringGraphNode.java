package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.List;

public class SimpleStringGraphNode<C extends StringGraphNode<?,C,?, SimpleStringGraphNode<C,P>>, P extends StringGraphNode<?,P, SimpleStringGraphNode<C,P>,?>>
        extends StringGraphNode<String, SimpleStringGraphNode<C,P>,C,P> {

    public SimpleStringGraphNode() {
        super();
    }

    public SimpleStringGraphNode(String value) {
        this();
        if (value.length() != 1)
            throw new IllegalArgumentException("Value of SimpleStringGraphNode must be of length 1");

        this.value = value;
    }

    @Override
    public void addForwardChild(C child) {
        throw new UnsupportedOperationException("Cannot add forward children to " + this.getClass().getName());
    }

    @Override
    public void removeChild(C child) {
        throw new UnsupportedOperationException("Cannot remove child from " + this.getClass().getName());
    }

    @Override
	public String toString() {
        return this.value;
	}

	@Override
    public List<String> getDenotation() {
        List<String> result = new ArrayList<>();
        if (this.toString().compareTo(ConstValues.MAX.name()) == 0) {
            result.add(ConstValues.ALL_STRINGS.name());
        } else if (this.toString().compareTo(ConstValues.MIN.name()) != 0) {
            result.add(this.toString());
        }
        // this this.value == MIN an empty list should be returned
        return result;
    }
}
