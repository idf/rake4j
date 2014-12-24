package rake4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rake4j.core.model.AbstractAlgorithm;
import rake4j.core.model.Document;
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

/**
 *
 * An Implementation of the RAKE (Rapid Automatic Keyword Extraction)
 * <i> Rose, Stuart, et al. "Automatic keyword extraction from individual
 * documents." Text Mining (2010): 1-20.
 * </i>
 *
 * This implementation is based on JATE https://code.google.com/p/jatetoolkit/
 * and on https://github.com/aneesha/RAKE, it gives similar results as the
 * python script provided a good stopword list with a punctuation list
 *
 * The numbers have been taken into account using JATE method. The algorithm
 * expects that the puntuaction marks are separated within a whitespace. 
 * " The red table , that is in front of you , is mine . "
 * To achieve this you should use a parser like OpenNLP, Illinois POS Tagger, 
 * Freeling parsers etc.
 * 
 * 
 * TODO: use POS tags to avoid verbs and other unwanted type of words in the 
 * process of keyword generation
 * 
 * @author Angel Conde Manjon
 */

public class Rake extends AbstractAlgorithm {
    private transient Document doc = null;
    private List<String> stopWordList = new ArrayList<>();
    transient private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<Pattern> regexList = new ArrayList<>();
    private List<String> punctList = new ArrayList<>();
    private int minNumberLetters = 1;
    
    public Rake() throws URISyntaxException {
        super(true, "RAKE");
        this.init();
    }
    
    public void loadDocument(Document doc) {
        this.doc = doc;
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
    
    private List<String> splitToSentences(String text) {
        String splitter = "[.!?,;\\t\\-\"\'\\(\\)\\\\]";
        return Arrays.asList(text.split(splitter));
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

    @Override
    public void run() {
        List<String> sentenceList = splitToSentences(doc.getText());
        List<String> phraseList = generateCandidateKeywords(sentenceList, regexList);
        Map<String, Float> wordScore = calculateWordScores(phraseList);
        List<Term> keywordCandidates = generateCandidateKeywordScores(phraseList, wordScore);
        Comparator<? super Term> cmp = (o1, o2) -> o1.getScore() > o2.getScore() ? -1 : o1.getScore() == o2.getScore() ? 0 : 1;
        List<Term> sortedKeywords = keywordCandidates.parallelStream().sorted(cmp).distinct().collect(toList());
        doc.setTermList(sortedKeywords);
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
