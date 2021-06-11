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
        System.out.println(SGNUtils.compact(root));
        //concat2.normalize();
        //System.out.println(root);
        System.out.println("------------AFTER-----------");
    }

    @Test
    public void testCompactionAlgorithm() {
        // case 1
        StringGraphNode<?> node1 = new OrStringGraphNode();
        node1.addForwardChild(new ConstStringGraphNode(ConstValues.MIN));
        node1.addForwardChild(new ConstStringGraphNode(ConstValues.MIN));
        StringGraphNode<?> result1 = SGNUtils.compact(node1);
        assert result1 instanceof ConstStringGraphNode && result1.getValue().equals(ConstValues.MIN);

        // case 2
        StringGraphNode<?> node2 = new OrStringGraphNode();
        node2.addForwardChild(new ConstStringGraphNode(ConstValues.MIN));
        node2.addForwardChild(new SimpleStringGraphNode("a"));
        node2.addForwardChild(new SimpleStringGraphNode("b"));
        StringGraphNode<?> result2 = SGNUtils.compact(node2);
        assert result2 instanceof OrStringGraphNode &&
                result2.getOutDegree() == 2 &&
                result2.getForwardNodes().get(0).getValue().equals("a") &&
                result2.getForwardNodes().get(1).getValue().equals("b");

        System.out.println(result2);
    }

    @Test
    public void testGetForwardPath() {
        StringGraphNode<?> node2 = new OrStringGraphNode();
        StringGraphNode<?> node2Concat = new ConcatStringGraphNode();
        node2.addForwardChild(new ConstStringGraphNode(ConstValues.MIN));
        StringGraphNode<?> node2Simple = new SimpleStringGraphNode("a");
        node2Concat.addForwardChild(new SimpleStringGraphNode("a"));
        node2Concat.addForwardChild(new SimpleStringGraphNode("b"));
        node2.addForwardChild(node2Concat);
        List<StringGraphNode<?>> forwardPath = SGNUtils.getForwardPath(node2, node2Simple);
        System.out.println(forwardPath);
    }
}
