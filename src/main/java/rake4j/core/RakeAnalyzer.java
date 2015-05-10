package rake4j.core;

import io.deepreader.java.commons.util.Displayer;
import io.deepreader.java.commons.util.IOHandler;
import io.deepreader.java.commons.util.Sorter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rake4j.core.analysis.en.KStemmer;
import rake4j.core.model.Document;
import rake4j.core.model.Term;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class RakeAnalyzer extends Analyzer {
    private List<String> stopWordList = new ArrayList<>();
    transient private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<Pattern> regexList = new ArrayList<>();
    private Pattern stopWordPat ;
    private List<String> punctList = new ArrayList<>();
    private int minNumberLetters = 1;
    private int minWordsForPhrase = 1;
    private KStemmer stemmer = new KStemmer();
    
    public RakeAnalyzer() throws URISyntaxException {
        super(true, "RAKE");
        this.init();
    }

    /**
     * @param pStopWords - a list of stopWords
     */
    public void loadStopWords(List<String> pStopWords) {
        stopWordList = pStopWords;
    }

    /**
     * @param pLoc - the location of the file where the stopwords are
     */
    public void loadStopWords(InputStream pLoc) {
        List<String> stops = new ArrayList<>();
        try {
            // List<String> lines = Files.readAllLines(pLoc, StandardCharsets.UTF_8);
            List<String> lines  = IOUtils.readLines(pLoc, StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if(line.charAt(0)!='#') {
                    for(String word: line.split("\\s+"))
                        stops.add(word);
                }
            }
            this.loadStopWords(stops);
        } catch (IOException ex) {
            logger.error("Error loading RAKE stopWordList from: " + pLoc, ex);
        }
    }

    private Pattern buildStopWordRegex(List<String> pStopWords) {
        StringBuilder sb = new StringBuilder();
        for (String string : pStopWords) {
            sb.append("\\b").append(string.trim()).append("(?![\\w-])").append("|");  // hyphen: -ish
        }
        String pattern = sb.substring(0, sb.length() - 1);
        Pattern pat = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return pat;
    }
    
    private List<String> separateToWords(String text, int minWordReturnSize) {
        String splitter = "[^a-zA-Z0-9_\\+/]";
        List<String> words = new ArrayList<>();
        for(String word: text.split(splitter)) {
            word = word.trim().toLowerCase();
            if(word.length()>=minWordReturnSize && !word.equals("") && !isNumber(word)) {
                words.add(word);
            }
        }
        return words;
    }
    
    List<String> splitToSentences(String text) {
        String splitter = "[\\.!?,:;\\t\"\'\\(\\)\\\\\\n@=~&\\+]+|\\s\\-(\\s)?|(\\s)?\\-\\s";
        return Arrays.asList(text.split(splitter));
    }

    Map<Integer, String> splitToSentencesWithOffsets(String text) {
        List<String> splitTexts = splitToSentences(text);
        return getOffsetsOfSplitString(text, splitTexts, 0);
    }

    Map<Integer, String> getOffsetsOfSplitString(String text, List<String> splitStrings, int initialOffset) {
        List<Integer> offsets = new ArrayList<>();
        int offset = 0;
        for(String item: splitStrings) {
            offset = text.indexOf(item, offset);
            offsets.add(offset);
            offset += item.length();
        }
        assert splitStrings.size()==offsets.size();

        Map<Integer, String> offset2item = new HashMap<>();
        for(int i=0; i<offsets.size(); i++) {
            offset2item.put(offsets.get(i)+initialOffset, splitStrings.get(i));
        }
        if(offset2item.size()!=offsets.size()) {
            logger.trace(offset2item.size()+" not equal to "+offsets.size());
            logger.trace("One possible issue is the usage of hyphen in the raw text");
        }
        return offset2item;
    }

    /**
     * As this method uses Regex for candidate generation, custom regex
     * expresions could be added using this method (uses Java Pattern/Matcher
     * mechanism)
     *
     * @param pat
     */
    public void addCustomRegex(Pattern pat) {
        regexList.add(pat);
    }

    /**
     * This method works better with a list of punctuation stop list,
     * for example for english, spanish and in general in latin based languages
     * the list could be (.,/{}[];:)
     *
     * @param pPunt - the string list to be added
     */
    public void loadPunctStopWord(List<String> pPunt) {
        punctList = pPunt;

    }
    
    /**
     * This method works better with a list of punctuation stop list, for
     * example for english, spanish and in general in latin based languages the
     * list could be (.,/{}[];:)
     * 
     * Notice: the escapes are automatically added
     * @param pLoc - the location of the file where the stopwords are
     */
    public void loadPunctStopWord(URI pLoc) {
        List<String> stops = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(pLoc), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if(line.charAt(0)!='#') {
                   stops.add(line);
                }
            }
            this.loadPunctStopWord(stops);
        } catch (IOException e) {
            logger.error("Error loading RAKE punctList from: " + pLoc, e);
        }
    }

    private Pattern buildPunctStopWordRegex(List<String> pPunctStop) {
        StringBuilder sb = new StringBuilder();
        for (String string : pPunctStop) {
            sb.append("\\").append(string.trim()).append("|");
        }
        String pattern = sb.substring(0, sb.length() - 1);
        Pattern pat = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return pat;
    }

    private List<String> generateCandidateKeywords(List<String> sentenceList, List<Pattern> stopwordPattern) {
        List<String> phraseList = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        for (String s : sentenceList) {
            for (Pattern pat : stopwordPattern) {
                Matcher matcher = pat.matcher(s.trim());
                while (matcher.find()) {
                    matcher.appendReplacement(sb, "|");
                }
                matcher.appendTail(sb);
                if (sb.length() > 0) {
                    s = sb.toString();
                }
                sb = new StringBuffer();
            }
            
            List<String> phrases = Arrays.asList(s.split("\\|"));
            phraseList.addAll(phrases.parallelStream().filter(phrase -> phrase.trim().length()>0).filter(phrase -> phrase.length() > 0).map(String::trim).collect(Collectors.toList()));
        }
        return phraseList;
    }

    Map<Integer, String> generateCandidateKeywords(Map<Integer, String> sentenceList, List<Pattern> regexList) {
        Map<Integer, String> phraseList = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        Iterator itr = sentenceList.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            Integer initialOffset = (Integer) entry.getKey();
            String s = (String) entry.getValue();

            for (Pattern pat: regexList) {
                Matcher matcher = pat.matcher(s.trim());
                while (matcher.find()) {
                    matcher.appendReplacement(sb, "|");
                }
                matcher.appendTail(sb);
                if (sb.length() > 0) {
                    s = sb.toString();
                }
                sb = new StringBuffer();
            }

            List<String> phrases = Arrays.asList(s.split("\\|"));
            phrases = phrases.parallelStream().filter(phrase -> phrase.trim().length()>0).map(String::trim).collect(Collectors.toList());
            Map<Integer, String> subPhraseList = getOffsetsOfSplitString((String) entry.getValue(), phrases, initialOffset);
            phraseList.putAll(subPhraseList);
        }
        return phraseList;
    }

    Map<Integer, String> adjoinKeywords(Map<Integer, String> phraseList, Pattern stopwordPattern, String text) {
        SortedSet<Integer> keys = new TreeSet<>(phraseList.keySet());
        boolean adjoined = false;
        Map<String, List<Pair<Integer, Integer>>> candidates = new HashMap<>();

        Iterator itr = keys.iterator();
        if(!itr.hasNext())
            return phraseList;

        Integer i;
        Integer j = (Integer) itr.next();
        String interior;
        while(itr.hasNext()) {
            i = j;
            j = (Integer) itr.next();
            int interior_start = i+phraseList.get(i).length();
            if(interior_start>j) {
                logger.error("Overlapping index when adjoining keywords: " + phraseList.get(i) + " & " + phraseList.get(j));
                continue;
            }
            interior = text.substring(interior_start, j);
            List<String> tokens = Arrays.asList(interior.split("\\s+"));
            if(tokens.parallelStream().map(
                    token -> stopwordPattern.matcher(token).replaceAll("")).allMatch(
                    token -> token.trim().length() == 0)
                    ) {
                logger.trace(interior);
                String candidate = text.substring(i, j+phraseList.get(j).length()).trim();
                if(!candidates.containsKey(candidate)) {
                    candidates.put(candidate, new ArrayList<>());
                }
                candidates.get(candidate).add(new ImmutablePair<>(i, j));
            }
        }

        for(Map.Entry<String, List<Pair<Integer, Integer>>> e: candidates.entrySet()) {
            if(e.getValue().size()>=2) {
                adjoined = true;
                for(Pair<Integer, Integer> p: e.getValue()) {
                    if(phraseList.containsKey(p.getLeft())) {
                        phraseList.put(p.getLeft(), e.getKey());
                        phraseList.remove(p.getRight());
                    }
                }
            }
        }
        if(adjoined) {  // recursive
            phraseList = adjoinKeywords(phraseList, stopwordPattern, text);
        }
        return phraseList;
    }

    /**
     * Interface KStemming Algorithm
     * Stemming will not change the offset information
     * @param phraseList
     * @return
     */
    private Map<Integer, String> stem(Map<Integer, String> phraseList) {
        Map<Integer, String> ret = new HashMap<>();
        for(Map.Entry<Integer, String> e: phraseList.entrySet()) {
            String phrase = e.getValue();
            List<String> stemmedWords = new ArrayList<>();
            for(String w: phrase.split("\\s+")) {
                try {
                    stemmedWords.add(this.stemmer.stem(w));
                }
                catch (ArrayIndexOutOfBoundsException ex) {
                    logger.warn(Displayer.display(ex));
                    stemmedWords.add(w);
                }
            }
            phrase = stemmedWords.stream().collect(Collectors.joining(" "));
            ret.put(e.getKey(), phrase);
        }
        return ret;
    }

    private Map<String, Float> calculateWordScores(List<String> phraseList) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        Map<String, Integer> wordDegree = new HashMap<>();
        
        for (String phrase : phraseList) {
            List<String> wordlist = separateToWords(phrase, minNumberLetters);
            int wordlistlength = wordlist.size();
            int wordlistdegree = wordlistlength-1;
            for (String word : wordlist) {
                if (!wordFrequency.containsKey(word)) {
                    wordFrequency.put(word, 1);
                } else {
                    int freq = wordFrequency.get(word)+1;
                    wordFrequency.put(word, freq);
                }

                
                if (!wordDegree.containsKey(word)) {
                    wordDegree.put(word, wordlistdegree);
                } else {
                    int deg = wordDegree.get(word)+wordlistdegree;
                    wordDegree.put(word, deg);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : wordDegree.entrySet()) {
            entry.setValue(entry.getValue() + wordFrequency.get(entry.getKey()));
        }

        Map<String, Float> wordScore = new HashMap<>();
        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            wordScore.put(entry.getKey(), wordDegree.get(entry.getKey()) / (wordFrequency.get(entry.getKey()) * 1.0f));
        }
        return wordScore;
    }

    private List<Term> generateCandidateKeywordScores(List<String> phraseList, Map<String, Float> wordScore) {
        List<Term> termList = new ArrayList<>();
        for (String phrase : phraseList) {
            List<String> words = separateToWords(phrase, minNumberLetters);
            float score = 0.0f;
            for (String word : words) {
                score += wordScore.get(word);
            }
            termList.add(new Term(phrase, score));
        }
        return termList;
    }

    private Map<Integer, String> filteredByLength(Map<Integer, String> phraseList, int minWords) {
        return phraseList.entrySet()
                .parallelStream()
                .filter(e -> e.getValue().split("\\s+").length>=minWords)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> filteredByLength(List<String> phraseList, int minWords) {
        return phraseList.parallelStream()
                .filter(e -> e.split("\\s+").length>=minWords)
                .collect(Collectors.toList());
    }

    private Map<Integer, Term> generateCandidateKeywordScores(Map<Integer, String> phraseList, Map<String, Float> wordScore) {
        Map<Integer, Term> termList = new HashMap<>();
        for (Map.Entry entry: phraseList.entrySet()) {
            List<String> words = separateToWords((String) entry.getValue(), minNumberLetters);
            float score = 0.0f;
            for (String word : words) {
                score += wordScore.get(word);
            }
            termList.put((Integer) entry.getKey(), new Term((String) entry.getValue(), score));
        }
        return termList;
    }

    /**
     * called after loading, just before run
     */
    public void init() throws URISyntaxException {
        this.loadStopWords(this.getClass().getClassLoader().getResourceAsStream("SmartStopListEn.txt"));
        
        if (stopWordList.isEmpty()) {
            logger.error("The method " + this.getName() + " requires a StopWordList to build the candidate list");
            return;
        }
        stopWordPat = buildStopWordRegex(stopWordList);
        regexList.add(stopWordPat);
        
        if (!punctList.isEmpty()) {
            Pattern pat2 = buildPunctStopWordRegex(punctList);
            regexList.add(pat2);
        }
    }

    public Pattern getStopWordPat() {
        return stopWordPat;
    }

    public void runWithoutOffset() {
        List<String> sentenceList = splitToSentences(doc.getText().toLowerCase());
        List<String> phraseList = generateCandidateKeywords(sentenceList, regexList);
        Map<String, Float> wordScore = calculateWordScores(phraseList);
        phraseList = filteredByLength(phraseList, minWordsForPhrase);
        List<Term> keywordCandidates = generateCandidateKeywordScores(phraseList, wordScore);
        Comparator<? super Term> cmp = (o1, o2) -> o1.getScore() > o2.getScore() ? -1 : o1.getScore() == o2.getScore() ? 0 : 1;
        List<Term> sortedKeywords = keywordCandidates.parallelStream().sorted(cmp).distinct().collect(toList());
        doc.setTermList(sortedKeywords);
    }

    @Override
    public void run() {
        String text = doc.getText().toLowerCase();
        Map<Integer, String> sentenceList = splitToSentencesWithOffsets(text);
        Map<Integer, String> phraseList = generateCandidateKeywords(sentenceList, regexList);
        phraseList = adjoinKeywords(phraseList, stopWordPat, text);
        phraseList = stem(phraseList);
        Map<String, Float> wordScore = calculateWordScores(new ArrayList<>(phraseList.values()));
        phraseList = filteredByLength(phraseList, minWordsForPhrase);
        Map<Integer, Term> keywordCandidates = generateCandidateKeywordScores(phraseList, wordScore);
        TreeMap<Integer, Term> sortedKeywords = Sorter.sortByValue(keywordCandidates, new Sorter.ValueComparator<Integer, Term>(keywordCandidates) {
            @Override
            public int compare(Integer a, Integer b) {
                try {
                    if (base.get(a).getScore() < base.get(b).getScore()) return 1;
                    else if (a.equals(b)) return 0;
                    else return -1;
                } catch (NullPointerException e) {
                    return -1;
                }
            }
        });
        doc.setTermMap(sortedKeywords);
        // top k keywords is processed in indexing phase
    }

    public static void run(String path) throws Exception {
        String text = IOHandler.read(path);
        RakeAnalyzer analyzer = new RakeAnalyzer();
        analyzer.setMinWordsForPhrase(2);
        Document doc = new Document(text);
        analyzer.loadDocument(doc);
        analyzer.runWithoutOffset();
        System.out.println(doc.termListToString(0.5));
    }

    /**
     * is number
     * @param string
     * @return
     */
    private  boolean isNumber(String string) {
        try {
            Double.parseDouble(string);
        }
        catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
    
    public int getMinNumberLetters() {
        return minNumberLetters;
    }

    public void setMinNumberLetters(int minNumberLetters) {
        this.minNumberLetters = minNumberLetters;
    }

    public int getMinWordsForPhrase() {
        return minWordsForPhrase;
    }

    public void setMinWordsForPhrase(int minWordsForPhrase) {
        this.minWordsForPhrase = minWordsForPhrase;
    }
}
