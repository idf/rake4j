package rake4j.core;

import junit.framework.TestCase;
import rake4j.core.model.Document;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Unit test for simple App.
 */
public class RakeAnalyzerTest extends TestCase {
    public void testRunWithoutOffset() throws URISyntaxException {
        String text = "Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types of systems and systems of mixed types.";
        String actual = "minimal generating sets\t8.666667\n" +
                "linear Diophantine equations\t8.5\n" +
                "minimal supporting set\t7.666667\n" +
                "minimal set\t4.666667\n" +
                "linear constraints\t4.5\n" +
                "natural numbers\t4.0\n" +
                "strict inequations\t4.0\n" +
                "nonstrict inequations\t4.0\n" +
                "Upper bounds\t4.0\n" +
                "mixed types\t3.6666665\n" +
                "considered types\t3.1666665\n" +
                "set\t2.0\n" +
                "types\t1.6666666\n" +
                "considered\t1.5\n" +
                "Compatibility\t1.0\n" +
                "systems\t1.0\n" +
                "Criteria\t1.0\n" +
                "compatibility\t1.0\n" +
                "system\t1.0\n" +
                "components\t1.0\n" +
                "solutions\t1.0\n" +
                "algorithms\t1.0\n" +
                "construction\t1.0\n" +
                "criteria\t1.0\n" +
                "constructing\t1.0\n" +
                "solving\t1.0\n";
        
        
        Document doc = new Document(text);
        RakeAnalyzer rake = new RakeAnalyzer();
        rake.loadDocument(doc);
        rake.runWithoutOffset();
        // System.out.println(doc.termListToString());
        assertEquals(actual, doc.termListToString());
    }

    public void testSplitSentences() throws URISyntaxException {
        RakeAnalyzer rake = new RakeAnalyzer();
        String text = "sentence 1....\n" +
                "sentence sentence 2\n" +
                "\n" +
                "\n" +
                "sentence 3";
        Map<Integer, String> map = rake.splitToSentencesWithOffsets(text);

        Iterator it = map.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            // System.out.println(pair.getKey()+": "+pair.getValue());
        }

        assert map.get(0).equals("sentence 1");
        assert map.get(15).equals("sentence sentence 2");
        assert map.get(37).equals("sentence 3");

    }
    public void testGetOffsetsOfSplitString() throws URISyntaxException {
        String text = "Compatibility of systems of linear constraints over the set of natural numbers.";
        text = text.toLowerCase();
        List<String> list = new ArrayList<>();
        list.add("compatibility");
        list.add("systems");
        RakeAnalyzer rake = new RakeAnalyzer();
        Map<Integer, String> map = rake.getOffsetsOfSplitString(text, list, 0);
        assert map.get(0).equals("compatibility");
        assert map.get(17).equals("systems");

    }
    public void testAdjoinKeywords() throws URISyntaxException {
        // String text = "Gang of Four. Gang of Four"; // won't fix, four is a stop word
        String text = "axis of evil of night. axis of evil of night. Something else";
        Document doc = new Document(text);
        RakeAnalyzer rake = new RakeAnalyzer();
        rake.loadDocument(doc);
        rake.run();
        assert doc.termMapToString().contains("axis of evil of night");

    }
    public void testRun() throws URISyntaxException {
        String text = "Compatibility of systems of linear constraints over the set of natural numbers. Criteria of compatibility of a system of linear Diophantine equations, strict inequations, and nonstrict inequations are considered. Upper bounds for components of a minimal set of solutions and algorithms of construction of minimal generating sets of solutions for all types of systems are given. These criteria and the corresponding algorithms for constructing a minimal supporting set of solutions can be used in solving all the considered types of systems and systems of mixed types.";
        String actual = "";
        Document doc = new Document(text);
        RakeAnalyzer rake = new RakeAnalyzer();
        rake.loadDocument(doc);
        rake.run();
        System.out.println(doc.termMapToString());
        // assertEquals(actual, doc.termListToString());
    }
}
