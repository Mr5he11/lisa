package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class SimpleStringGraphNode extends StringGraphNode<String> {

    enum ConstValue {
        MAX,
        MIN,
        EMPTY
    }
    
    /**
     *  TODO maybe we can remove the {@link ConstValue} enum and use these 3 fields as identifiers of MAX,MIN,EMPTY.
     *  
     *  in fact, the enum has package visibility, so it is nearly useless 
     */
    public static final StringGraphNode MAX = new SimpleStringGraphNode(ConstValue.MAX);
    public static final StringGraphNode MIN = new SimpleStringGraphNode(ConstValue.MIN);
    public static final StringGraphNode EMPTY = new SimpleStringGraphNode(ConstValue.EMPTY);

    public SimpleStringGraphNode(ConstValue constValue) {
        this(constValue.name(), false);
    }

    public SimpleStringGraphNode(String value) {
       this(value,true);
    }

    public SimpleStringGraphNode(String value, Collection<StringGraphNode> parents) {
        this(value);
        this.parents.addAll(parents);
    }

    private SimpleStringGraphNode(String value, Boolean checkLen) {
        if (checkLen) {
            if (value.length() != 1) throw new IllegalArgumentException("Value of SimpleStringGraphNode must be of length 1");
        }

        this.value = value;
        this.children = new ArrayList<>();
        this.parents = new ArrayList<>();
    }

    @Override
	public String toString() {
        return this.value;
	}
}
