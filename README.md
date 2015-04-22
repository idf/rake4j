rake4j
======
This is a re-write of [Python RAKE](https://github.com/aneesha/RAKE) in Java.  

An implementation of the Rapid Automatic Keyword Extraction (RAKE) algorithm as described in:  [Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010). Automatic Keyword Extraction from Individual Documents](http://scholar.google.com.sg/scholar?q=Automatic+Keyword+Extraction+from+Individual+Documents&btnG=&hl=en&as_sdt=0%2C5&as_vis=1)

#Run
##Sample
Normal run 
```java
        Document doc = new Document(text);
        RakeAnalyzer rake = new RakeAnalyzer();
        rake.loadDocument(doc);
        rake.runWithoutOffset();
        System.out.println(doc.termListToString());
```
Run with offset information and stemming 
```java
        Document doc = new Document(text);
        RakeAnalyzer rake = new RakeAnalyzer();
        rake.loadDocument(doc);
        rake.run();
        System.out.println(doc.termMapToString());
```
#Features
Recognized keywords from the algorithm based on stop words
* Adjoining keywords to recognized "axis of evil".
* KStemming algorithm ported from Lucene, to stem "university students" to "university student".
* Construct index of keywords with term frequency `tf` and document frequency `df`.

#Dependencies
In pom.xml, another custom maven module dependency is required:
```xml
        <dependency>
            <groupId>io.deepreader.java.commons</groupId>
            <artifactId>commons-util</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
You can get the module manually by:
```
git clone https://github.com/idf/commons-util
```
, which is hosted [here](https://github.com/idf/commons-util).

#References
[Python RAKE](https://github.com/aneesha/RAKE)  
[Python RAKE (forked)](https://github.com/idf/RAKE)  
[Java RAKE](https://github.com/Neuw84/RAKE-Java)  
