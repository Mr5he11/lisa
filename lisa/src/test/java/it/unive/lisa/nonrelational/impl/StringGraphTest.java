package it.unive.lisa.nonrelational.impl;

import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph.*;
import org.junit.Test;

import java.util.List;

public class StringGraphTest {

    @Test
    public void testStringGraph() {
        StringGraphNode<?> normalized = null;
        StringGraphNode<?> concat1 = StringGraphNode.create("ipsum");
        StringGraphNode<?> concat2 = StringGraphNode.create("lorem");
        StringGraphNode<?> concat3 = StringGraphNode.create("dolor");
        StringGraphNode<?> concat4 = new ConcatStringGraphNode();
        concat4.addForwardChild(new ConstStringGraphNode(ConstValues.MAX));
        concat4.addForwardChild(new ConstStringGraphNode(ConstValues.MAX));
        OrStringGraphNode root = new OrStringGraphNode();
        root.addForwardChild(concat1);
        root.addForwardChild(concat2);
        concat2.addForwardChild(concat3);
        concat1.addBackwardChild(root);
        concat3.addForwardChild(new ConstStringGraphNode(ConstValues.MAX));
        System.out.println("-----------BEFORE-----------");
        //concat2.normalize();
        System.out.println(root);
        System.out.println("------------AFTER-----------");
        try {
            normalized = StringGraphNode.deepClone(root);
        } catch(Exception e) {
            System.out.println(e);
        }

        // normalized.normalize();
        // System.out.println(root.getChildren());
        System.out.println(normalized);
    }
}
