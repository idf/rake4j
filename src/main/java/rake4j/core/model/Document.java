package rake4j.core.model;

/*
 *    Document.java
 *    Copyright (C) 2014 Angel Conde, neuw84 at gmail dot com
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * A document represents the piece of a corpus containing text.
 *
 * @author Angel Conde Manjon
 */
public class Document {
    private transient String text;
    private transient String path;
    private transient List<LinkedList<Token>> tokenList;
    private transient List<Term> termList = new ArrayList<>();
    private transient static final Logger logger = LoggerFactory.getLogger(Document.class);

    public Document(String text) {
        this.text = text;
    }

    /**
     * @return the tokenList
     */
    public List<LinkedList<Token>> getTokenList() {
        return tokenList;
    }

    /**
     * @param tokenList the tokenList to set
     */
    public void List(List<LinkedList<Token>> tokenList) {
        this.tokenList = tokenList;
    }

    /**
     *
     * @return
     */
    public List<Term> getTermList() {
        return termList;
    }

    /**
     * Tries to convert the content of this document to UTF-8 using java
     * CharsetDecoders
     */
    public void convertToUTF8() {
        FileInputStream istream = null;
        Writer out = null;
        try {
            istream = new FileInputStream(path);
            BufferedInputStream in = new BufferedInputStream(istream);
            CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
            charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE);
            charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            Reader inputReader = new InputStreamReader(in, charsetDecoder);
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputReader, writer);
            String theString = writer.toString();
            FileUtils.deleteQuietly(new File(path));
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
            out.write(theString);
            out.close();
//            System.out.println("");
        } catch (FileNotFoundException ex) {
            logger.error("Error converting the file to utf8", ex);
        } catch (IOException ex) {
            logger.error("Error converting the file to utf8", ex);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (istream != null) {
                    istream.close();
                }
            } catch (IOException ex) {
                logger.error("Error converting the file to utf8", ex);
            }
        }

    }

    public String termListToString() {
        StringBuffer sb = new StringBuffer();
        for(Term t: termList) {
            sb.append(t.toString()+"\n");
        }
        return sb.toString();
    }
    public void setTermList(List<Term> termList) {
        this.termList = termList;
    }

    public String getText() {
        return text;
    }
}
