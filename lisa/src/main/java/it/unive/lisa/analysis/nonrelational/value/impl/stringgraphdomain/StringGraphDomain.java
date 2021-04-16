package it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.NonRelationalDomain;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;

public class StringGraphDomain extends BaseNonRelationalValueDomain {
    @Override
    protected BaseLattice lubAux(BaseLattice other) throws SemanticException {
        return null;
    }

    @Override
    protected BaseLattice wideningAux(BaseLattice other) throws SemanticException {
        return null;
    }

    @Override
    protected boolean lessOrEqualAux(BaseLattice other) throws SemanticException {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public Lattice lub(Lattice other) throws SemanticException {
        return null;
    }

    @Override
    public Lattice widening(Lattice other) throws SemanticException {
        return null;
    }

    @Override
    public boolean lessOrEqual(Lattice other) throws SemanticException {
        return false;
    }

    @Override
    public Lattice top() {
        return null;
    }

    @Override
    public Lattice bottom() {
        return null;
    }

    @Override
    public NonRelationalDomain eval(SymbolicExpression expression, FunctionalLattice environment, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public SemanticDomain.Satisfiability satisfies(SymbolicExpression expression, FunctionalLattice environment, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public FunctionalLattice assume(FunctionalLattice environment, SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
        return null;
    }

    @Override
    public NonRelationalDomain glb(NonRelationalDomain other) throws SemanticException {
        return null;
    }

    @Override
    public String representation() {
        return null;
    }
}
