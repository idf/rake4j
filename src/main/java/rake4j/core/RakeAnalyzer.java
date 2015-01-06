package rake4j.core;

import io.deepreader.java.commons.util.Sorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rake4j.core.model.Analyzer;
import rake4j.core.model.Term;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class RakeAnalyzer extends Analyzer {
    private List<String> stopWordList = new ArrayList<>();
    transient private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<Pattern> regexList = new ArrayList<>();
    private List<String> punctList = new ArrayList<>();
    private int minNumberLetters = 1;
    
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
    public void loadStopWords(URI pLoc) {
        List<String> stops = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(pLoc), StandardCharsets.UTF_8);
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
            sb.append("\\b").append(string.trim()).append("\\b").append("|");
            // TODO, hyphen
        }
        String pattern = sb.substring(0, sb.length() - 1);
        Pattern pat = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return pat;
    }
    
    private List<String> separateToWords(String text, int minWordReturnSize) {
        String splitter = "[^a-zA-Z0-9_\\+\\-/]";
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
        String splitter = "[\\.!?,;\\t\\-\"\'\\(\\)\\\\\\n]+";
        return Arrays.asList(text.split(splitter));
    }

    Map<Integer, String> splitToSentencesWithOffsets(String text) {
        List<String> splitTexts = splitToSentences(text);
        return getOffsetsOfSplitString(text, splitTexts, 0);
    }

    private Map<Integer, String> getOffsetsOfSplitString(String s, List<String> splitStrings, int initialOffset) {
        List<Integer> offsets = new ArrayList<>();
        int offset = -1;
        for(String item: splitStrings) {
            offset = s.indexOf(item, offset+1);
            offsets.add(offset);
        }
        assert splitStrings.size()==offsets.size();

        Map<Integer, String> offset2item = new HashMap<>();
        for(int i=0; i<offsets.size(); i++) {
            offset2item.put(offsets.get(i)+initialOffset, splitStrings.get(i));
        }
        assert offset2item.size()==offsets.size();
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
            for (String phrase : phrases) {
                if (phrase.trim().length() > 0) {
                    if(phrase.length()>0)
                        phraseList.add(phrase.trim());
                }
            }
        }
        return phraseList;
    }

    Map<Integer, String> generateCandidateKeywords(Map<Integer, String> sentenceList, List<Pattern> stopwordPattern) {
        Map<Integer, String> phraseList = new HashMap<>();
        StringBuffer sb = new StringBuffer();
        Iterator itr = sentenceList.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            Integer initialOffset = (Integer) entry.getKey();
            String s = (String) entry.getValue();

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
            List<String> temp = new ArrayList<>();
            for (String phrase : phrases) {
                if (phrase.trim().length()>0) {
                    temp.add(phrase.trim());
                }
            }
            Map<Integer, String> subPhraseList = getOffsetsOfSplitString(s, temp, initialOffset);
            phraseList.putAll(subPhraseList);
        }
        return phraseList;
    }

    private Map<String, Float> calculateWordScores(List<String> phraseList) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        Map<String, Integer> wordDegree = new HashMap<>();
        
        for (String phrase : phraseList) {
            List<String> wordlist = separateToWords(phrase, minNumberLetters);
            int wordlistlength = wordlist.size();
            int wordlistdegree = wordlistlength-1;
            for (String word : wordlist) {
                if (wordFrequency.containsKey(word)==false) {
                    wordFrequency.put(word, 1);
                } else {
                    int freq = wordFrequency.get(word)+1;
                    wordFrequency.put(word, freq);
                }

                
                if (wordDegree.containsKey(word)==false) {
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
        this.loadStopWords(this.getClass().getResource("/SmartStopListEn.txt").toURI());
        
        if (stopWordList.isEmpty()) {
            logger.error("The method " + this.getName() + " requires a StopWordList to build the candidate list");
            return;
        }
        Pattern pat = buildStopWordRegex(stopWordList);
        regexList.add(pat);
        
        if (!punctList.isEmpty()) {
            Pattern pat2 = buildPunctStopWordRegex(punctList);
            regexList.add(pat2);
        }
    }

    public void runWithoutOffset() {
        List<String> sentenceList = splitToSentences(doc.getText());
        List<String> phraseList = generateCandidateKeywords(sentenceList, regexList);
        Map<String, Float> wordScore = calculateWordScores(phraseList);
        List<Term> keywordCandidates = generateCandidateKeywordScores(phraseList, wordScore);
        Comparator<? super Term> cmp = (o1, o2) -> o1.getScore() > o2.getScore() ? -1 : o1.getScore() == o2.getScore() ? 0 : 1;
        List<Term> sortedKeywords = keywordCandidates.parallelStream().sorted(cmp).distinct().collect(toList());
        doc.setTermList(sortedKeywords);
    }

    @Override
    public void run() {
        Map<Integer, String> sentenceList = splitToSentencesWithOffsets(doc.getText());
        Map<Integer, String> phraseList = generateCandidateKeywords(sentenceList, regexList);
        Map<String, Float> wordScore = calculateWordScores(new ArrayList<>(phraseList.values()));
        Map<Integer, Term> keywordCandidates = generateCandidateKeywordScores(phraseList, wordScore);
        TreeMap<Integer, Term> sortedKeywords = Sorter.sortByValues(keywordCandidates, new Sorter.ValueComparator<Integer, Term>(keywordCandidates) {
            @Override
            public int compare(Integer a, Integer b) {
                if (base.get(a).getScore()<base.get(b).getScore()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        doc.setTermMap(sortedKeywords);
        // TODO top k keywords
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
}
