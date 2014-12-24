rake4j
======
This is a re-write of [Python RAKE](https://github.com/aneesha/RAKE) in Java.  

An implementation of the Rapid Automatic Keyword Extraction (RAKE) algorithm as described in:  [Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010). Automatic Keyword Extraction from Individual Documents](http://scholar.google.com.sg/scholar?q=Automatic+Keyword+Extraction+from+Individual+Documents&btnG=&hl=en&as_sdt=0%2C5&as_vis=1)

#Run
##Minimal
```java
        Document doc = new Document(text);
        Rake rake = new Rake();
        rake.loadDocument(doc);
        rake.run();
        System.out.println(doc.termListToString());
```

#References
[Python RAKE](https://github.com/aneesha/RAKE)  
[Python RAKE (forked)](https://github.com/zhangdanyangg/RAKE)  
[Java RAKE](https://github.com/Neuw84/RAKE-Java)
