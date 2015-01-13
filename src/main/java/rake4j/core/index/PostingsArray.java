package rake4j.core.index;

import io.deepreader.java.commons.util.Displayer;

import java.io.Serializable;

/**
 * User: Danyang
 * Date: 1/6/2015
 * Time: 20:28
 */
public class PostingsArray implements Serializable {
    int df;
    int tf;

    @Override
    public String toString() {
        return Displayer.toString(this, " ");
    }
}
