package it.unive.lisa.nonrelational.impl;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSA;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.StringGraphDomain;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import org.junit.Test;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

public class StringGraphDomainTest extends AnalysisTestExecutor {

    @Test
    public void testStringGraph() throws ParsingException, AnalysisException {
        LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true).setAbstractState(
                getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new StringGraphDomain()));
        conf.setWorkdir("test-outputs/string-graph-abstract-domain");
        Program program = IMPFrontend.processFile("imp-testcases/string-graph-abstract-domain/string-graphs.imp");
        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}
