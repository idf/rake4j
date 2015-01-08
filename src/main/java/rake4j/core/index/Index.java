package rake4j.core.index;

import io.deepreader.java.commons.util.Displayer;
import rake4j.core.model.Document;
import rake4j.core.model.Term;

import java.io.Serializable;
import java.util.*;

/**
 * User: Danyang
 * Date: 1/6/2015
 * Time: 20:15
 */
public class Index implements Serializable {
    Map<String, PostingsArray> invertedIndex = new HashMap<>();

    public void processDoc(Document doc, float topPercentage) {
        TreeMap<Integer, Term> termMap = doc.getTermMap();
        Set<String> repeated = new HashSet<>();

        int cnt = 0;
        float upper = termMap.size()*topPercentage;
        Iterator itr = termMap.entrySet().iterator();
        while(itr.hasNext() && cnt<=upper) {
            Map.Entry e = (Map.Entry) itr.next();
            cnt += 1;
            Term t = (Term) e.getValue();
            String s = t.getTermText();
            if(!invertedIndex.containsKey(s)) {
                invertedIndex.put(s, new PostingsArray());
            }
            if(!repeated.contains(s)) {
                invertedIndex.get(s).df += 1;
                repeated.add(s);
            }
            invertedIndex.get(s).tf += 1;
        }

    }

    @Override
    public String toString() {
        return Displayer.display(this.invertedIndex);
    }

    public Integer docFreq(String term) {
        if(invertedIndex.containsKey(term)) {
            return invertedIndex.get(term).df;
        }
        return 0;
    }

}
