package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.stringgraph;

import java.util.Collection;
import java.util.HashSet;

public abstract class StringGraphNode {
    protected Object value;
    protected HashSet<StringGraphNode> children;
    protected HashSet<StringGraphNode> parents;

    public int getOutDegree() {
        return this.children.size();
    }

    public int getInDegree() {
        return this.parents.size();
    }

    public boolean isLeaf() {
        return this.getOutDegree() == 0;
    }

    public boolean isRoot() {
        return this.getInDegree() == 0;
    }

    public void setChildren(Collection<StringGraphNode> children) {
        this.children = new HashSet<>(children);
    }

    public void setParents(Collection<StringGraphNode> parents) {
        this.parents = new HashSet<>(parents);
    }

    public HashSet<StringGraphNode> getChildren() {
        return this.children;
    }

    public HashSet<StringGraphNode> getParents() {
        return this.parents;
    }

    public void addChild(StringGraphNode child) {
        this.children.add(child);
    }

    public void addParent(StringGraphNode parent) {
        this.parents.add(parent);
    }

    public void removeChild(StringGraphNode child) {
        this.children.remove(child);
    }

    public void removeParent(StringGraphNode parent) {
        this.parents.remove(parent);
    }
}
