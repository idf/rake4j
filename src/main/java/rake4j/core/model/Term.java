package rake4j.core.model;

/*
 *    Term.java
 *    Copyright (C) 2013 Angel Conde, neuw84 at gmail dot com
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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A termText represents a candidate of the termText extraction methods, it's need that it will
 * pass a validation with a knowledge base before knowing if it is a topic
 * A termText contains an String with the termText's text and a score if the algorithm
 * used for extracting the termText has one. (if not the score must be -1)
 *
 * @author Angel Conde Manjon
 */

public class Term {

    private String termText;
    private float score;
    private List<Integer> offsets = new ArrayList<>();


    /**
     *
     */
    public Term() {

    }

    /**
     *
     * @param pTerm
     */
    
        
    public Term(String pTerm) {
        termText = pTerm;
        score = -1;

    }

    /**
     *
     * @param pTerm
     * @param pScore
     */
    public Term(String pTerm, float pScore) {
        termText = pTerm;
        score = pScore;
    }

    /**
     * @return the extracted termterm
     */
    public String getTermText() {
        return termText;
    }

    /**
     * @param termText the termText to set
     */
    public void setTermText(String termText) {
        this.termText = termText;
    }

    /**
     * @return the score
     */
    public float getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(float score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return termText + "\t" + score;
    }

   
    @Override
    public boolean equals(Object pObject) {
        if (pObject instanceof Term) {
            return this.termText.equalsIgnoreCase(((Term) pObject).getTermText());
        } else {
            return false;

        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.termText);
        return hash;
    }

    public List<Integer> getOffsets() {
        return offsets;
    }

    public void setOffsets(List<Integer> offsets) {
        this.offsets = offsets;
    }
}
