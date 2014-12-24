rake4j
======

A Java implementation of the Rapid Automatic Keyword Extraction (RAKE) algorithm as described in: Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010). Automatic Keyword Extraction from Individual Documents. In M. W. Berry & J. Kogan (Eds.), Text Mining: Theory and Applications: John Wiley & Sons. [Link](http://scholar.google.com.sg/scholar?q=Automatic+Keyword+Extraction+from+Individual+Documents&btnG=&hl=en&as_sdt=0%2C5&as_vis=1)

The source code is released under the MIT License.

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
[Python RAKE](https://github.com/zhangdanyangg/RAKE)  
[Java RAKE](https://github.com/Neuw84/RAKE-Java)
