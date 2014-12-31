rake4j
======
This is a re-write of [Python RAKE](https://github.com/aneesha/RAKE) in Java.  

An implementation of the Rapid Automatic Keyword Extraction (RAKE) algorithm as described in:  [Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010). Automatic Keyword Extraction from Individual Documents](http://scholar.google.com.sg/scholar?q=Automatic+Keyword+Extraction+from+Individual+Documents&btnG=&hl=en&as_sdt=0%2C5&as_vis=1)

#Run
##Sample
Normal run 
```java
        Document doc = new Document(text);
        Rake rake = new Rake();
        rake.loadDocument(doc);
        rake.runWithoutOffset();
        System.out.println(doc.termListToString());
```
Run with offset information 
```java
        Document doc = new Document(text);
        Rake rake = new Rake();
        rake.loadDocument(doc);
        rake.run();
        System.out.println(doc.termMapToString());
```
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
git clone https://github.com/zhangdanyangg/commons-util
```
, which is hosted [here](https://github.com/zhangdanyangg/commons-util).

#References
[Python RAKE](https://github.com/aneesha/RAKE)  
[Python RAKE (forked)](https://github.com/zhangdanyangg/RAKE)  
[Java RAKE](https://github.com/Neuw84/RAKE-Java)
