package it.unive.lisa.nonrelational.impl;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.AnalysisTestExecutor;
import it.unive.lisa.LiSAConfiguration;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.nonrelational.value.impl.stringgraphdomain.StringGraphDomain;
import it.unive.lisa.imp.ParsingException;
import org.junit.Test;

import static it.unive.lisa.LiSAFactory.getDefaultFor;

public class StringGraphDomainTest extends AnalysisTestExecutor {

    @Test
    public void testStringGraph() throws ParsingException, AnalysisException {
        LiSAConfiguration conf = new LiSAConfiguration().setDumpAnalysis(true).setAbstractState(
                getDefaultFor(AbstractState.class, getDefaultFor(HeapDomain.class), new StringGraphDomain()));

        perform("string-graph-abstract-domain", "string-graphs.imp", conf);
    }
}
