package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.Collection;
import java.util.HashSet;

public class SimpleStringGraphNode extends StringGraphNode{

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

    public SimpleStringGraphNode() {
        this.value = ConstValue.EMPTY;
        this.children = new HashSet<>();
        this.parents = new HashSet<>();
    }

    public SimpleStringGraphNode(ConstValue value) {
    	this();
    	this.value = value;
    }

    public SimpleStringGraphNode(String value) {
        this();
        if (value.length() != 1) throw new IllegalArgumentException("Value of SimpleStringGraphNode must be of length 1");
        this.value = value;
    }

    public SimpleStringGraphNode(String value, Collection<StringGraphNode> parents) {
        this(value);
        this.parents.addAll(parents);
    }
    

    @Override
	public String toString() {
		if (this.value != null) {
			return this.value instanceof ConstValue ? ((ConstValue)this.value).name() : this.value.toString();
		}
		return null;
	}
}
