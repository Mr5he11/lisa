package it.unive.lisa.nonrelational.impl;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.ConcatStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.OrStringGraphNode;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.StringGraphNode;
import org.junit.Test;

import java.util.List;

public class StringGraphTest {

    @Test
    public void testStringGraph() {
        StringGraphNode<?> concat1 = StringGraphNode.create("ipsum");
        StringGraphNode<?> concat2 = StringGraphNode.create("lorem");
        StringGraphNode<?> concat3 = StringGraphNode.create("dolor");
        OrStringGraphNode root = new OrStringGraphNode();
        root.addForwardChild(concat1);
        root.addForwardChild(concat2);
        concat2.addForwardChild(concat3);
        concat1.addBackwardChild(root);
        root.normalize();
        System.out.println(root);
    }
}
