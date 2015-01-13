package rake4j.core;

import rake4j.core.index.Index;
import rake4j.core.model.Document;

/**
 * User: Danyang
 * Date: 1/6/2015
 * Time: 20:16
 */
public class IndexWriter {
    private Analyzer analyzer;
    private Index index;
    private float percentage;

    public IndexWriter(Index index, Analyzer analyzer, float percentage) {
        this.analyzer = analyzer;
        this.percentage = percentage;
        this.index = index;
    }

    public void addDocument(Document doc) {
        addDocument(doc, analyzer);
    }

    void addDocument(Document doc, Analyzer analyzer) {
        analyzer.loadDocument(doc);
        analyzer.run();
        index.processDoc(doc, percentage);
    }
}