package org.onosproject.FNL;

import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by mao on 12/5/15.
 */
public class tsLoopPacket {

    public Map<Criterion.Type, Criterion> match;
    public List<FlowEntry> pathFlow;
    public List<Link> pathLink;

    tsLoopPacket(){
    }

    /**
     *
     * @return constant SetHeader_*
     */
    public static final int SetHeader_SUCCESS = 40123;
    public static final int SetHeader_OVERRIDE = 40110;
    public static final int SetHeader_FAILURE = 0;
    public int setHeader(Criterion criterion){
        match.containsKey(crit)
        return SetHeader_FAILURE;
    }


    public boolean delHeader(){return false;};
    public boolean existHeader(){return false;};

    public boolean addPathFlow(){return false;};
    public boolean delPathFlow(){return false;};

    public boolean addPathLink(){return false;};
    public boolean delPathLink(){return false;};


    /**
     * @param collision: as return value: if criterion contain mutiple criterion with same type, it is true
     * @return tsLoopPacket: if anyone is SetHeader_FAILURE, return null
     */
    public static tsLoopPacket matchBuilder(Iterable<Criterion> criterion, Boolean collision){

        if(null != collision)
            collision = true;

        tsLoopPacket pkt = new tsLoopPacket();

        for(Criterion criteria : criterion){
            if (null == pkt)
                break;

            switch(pkt.setHeader(criteria)){
                case SetHeader_SUCCESS:
                    break;
                case SetHeader_OVERRIDE:
                    if(null != collision)
                        collision = false;
                    break;
                case SetHeader_FAILURE:
                    pkt = null;
                    break;
            }
        }

        return pkt;
    }

}
