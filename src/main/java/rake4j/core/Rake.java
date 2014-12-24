package rake4j.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rake4j.core.model.AbstractAlgorithm;
import rake4j.core.model.Document;
import rake4j.core.model.Term;

import java.io.IOException;
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
    private final transient List<Term> termList; 
    private List<String> stopWordList;
    transient private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private List<Pattern> regexList = null;
    private List<String> punctList;
    private int minNumberOfletters = 2;
    
    public Rake() {
        super(true, "RAKE");
        termList = super.getTermList();
        stopWordList = new ArrayList<>();
        regexList = new ArrayList<>();
        punctList = new ArrayList<>();
    }

    @Override
    public void init(Document pDoc, String pPropsDir) {
        this.setDocument(pDoc);
        doc = pDoc;
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
    public void loadStopWords(String pLoc) {
        List<String> stops = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(pLoc), StandardCharsets.UTF_8);
            for (String line : lines) {
                line.trim();
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
     * @param pLoc - the location of the file where the stopwords are
     */
    public void loadPunctStopWord(String pLoc) {
        List<String> stops = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(pLoc), StandardCharsets.UTF_8);
            for (String line : lines) {
                line.trim();
                if(line.charAt(0)!='#') {
                    for(String word: line.split("\\s+"))
                        stops.add(word);
                }
            }
            this.loadPunctStopWord(stops);
        } catch (IOException ex) {
            logger.error("Error loading RAKE punctList from: " + pLoc, ex);
        }
    }

    private Pattern buildPunctStopWord(List<String> pPunctStop) {
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
            List<String> cands = Arrays.asList(s.split("\\|"));
            for (String string1 : cands) {
                if (string1.trim().length() > 0) {
                    String[] p = string1.trim().split("\\s+");
                    if (string1.length() > 2 && p.length > 1 && !isNumber(string1)) {
                        phraseList.add(string1.trim());
                    }
                }
            }
        }
        return phraseList;
    }
    
    
    
    
    @Override
    public void run() {
        if (stopWordList.isEmpty()) {
            logger.error("The method " + this.getName() + " requires a StopWordList to build the candidate list");
            return;
        }
        
        Map<String, Integer> wordfreq = new HashMap<>();
        Map<String, Integer> worddegree = new HashMap<>();
        Map<String, Float> wordscore = new HashMap<>();
        Pattern pat = buildStopWordRegex(stopWordList);
        regexList.add(pat);
        if (!punctList.isEmpty()) {
            Pattern pat2 = buildPunctStopWord(punctList);
            regexList.add(pat2);
        }
        List<String> candidates = generateCandidateKeywords(doc.getSentenceList(), regexList);
        for (String phrase : candidates) {
            String[] wordlist = phrase.split("\\s+");
            int wordlistlength = wordlist.length;
            int wordlistdegree = wordlistlength - 1;
            for (String word : wordlist) {
                int freq;
                if (wordfreq.containsKey(word)==false) {
                    wordfreq.put(word, 1);
                } else {
                    freq = wordfreq.get(word) + 1;
                    wordfreq.remove(word);
                    wordfreq.put(word, freq);
                }

                if (worddegree.containsKey(word)==false) {
                    worddegree.put(word, wordlistdegree);
                } else {
                    int deg = worddegree.get(word) + wordlistdegree;
                    worddegree.remove(word);
                    worddegree.put(word, deg);
                }
            }
        }
        for (Map.Entry<String, Integer> entry : worddegree.entrySet()) {
            entry.setValue(entry.getValue() + wordfreq.get(entry.getKey()));
        }
        List<Term> termLi = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : wordfreq.entrySet()) {
            wordscore.put(entry.getKey(), worddegree.get(entry.getKey()) / (wordfreq.get(entry.getKey()) * 1.0f));
        }
        for (String phrase : candidates) {
            String[] words = phrase.split("\\s+");
            float score = 0.0f;
            for (String word : words) {
                score += wordscore.get(word);
            }
            termLi.add(new Term(phrase, score));
        }
        Comparator<? super Term> sorter = (o1, o2) -> o1.getScore() > o2.getScore() ? -1 : o1.getScore() == o2.getScore() ? 0 : 1;
        List<Term> orderedList = termLi.parallelStream().sorted(sorter).distinct().collect(toList());
        doc.setTermList(orderedList);
    
    }

    /**
     *
     * Returns the current (Default 2)
     *
     * @return the minNumberOfletters required to a word to be included
     */
    public int getMinNumberOfletters() {
        return minNumberOfletters;
    }

    /**
     * Default 2
     *
     * @param minNumberOfletters the minNumberOfletters to set to a word to be
     * included
     */
    public void setMinNumberOfletters(int minNumberOfletters) {
        this.minNumberOfletters = minNumberOfletters;
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
}
