package org.onosproject.fnl.impl;

/**
 * Created by mao on 12/22/15.
 */


/**
 *
 * @param <M>
 */
public class TsReturn<M> {
    private M ret = null;

    /**
     *
     * @param value
     */
    public void setValue(M value) {
        ret = value;
    }

    /**
     *
     * @return
     */
    public M getValue() {
        return ret;
    }

    /**
     *
     * @return
     */
    public boolean isNull() {
        return null == ret;
    }
}
