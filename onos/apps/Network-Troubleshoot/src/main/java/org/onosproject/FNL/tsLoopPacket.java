package org.onosproject.FNL;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by mao on 12/5/15.
 */
public class tsLoopPacket implements Serializable {

    private Map<Criterion.Type, Criterion> match;
    private Stack<FlowEntry> pathFlow;
    private Stack<Link> pathLink; // TODO - Upgrade check - To make sure it include just Link but not EdgeLink

    private tsLoopPacket() {
        match = new HashMap<Criterion.Type, Criterion>();
        pathFlow = new Stack<FlowEntry>();
        pathLink = new Stack<Link>();
    }


    public tsLoopPacket copyPacket() {
        Object ret = null;
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
            objOut.writeObject(this);
            objOut.close();

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream objIn = new ObjectInputStream(byteIn);
            ret = objIn.readObject();
            objIn.close();
        } catch (Exception e) { // ClassNotFoundException | IOException | anything else?
            e.printStackTrace();
        }
        return (tsLoopPacket)ret;
    }


    /**
     * @return constant SetHeader_*
     */
    public static final int SetHeader_SUCCESS = 40123;
    public static final int SetHeader_OVERRIDE = 40110;
    public static final int SetHeader_FAILURE = 0;

    public int setHeader(Criterion criterion) {
        boolean hasKey = match.containsKey(criterion.type());

        match.put(criterion.type(), criterion);

        return true == hasKey ? SetHeader_OVERRIDE : SetHeader_SUCCESS;
    }


    public boolean delHeader(Criterion.Type criterionType) {
        if (false == match.containsKey(criterionType)) {
            return false;
        } else {
            match.remove(criterionType);
            return true;
        }
    }


    public Criterion getHeader(Criterion.Type criterionType) {
        return match.get(criterionType); // TODO - check is null when the key is not contained?
    }

    public boolean existHeader(Criterion.Type criterionType) {
        return match.containsKey(criterionType);
    }


    public boolean pushPathFlow(FlowEntry entry) {
        pathFlow.push(entry);
        return true;
    }


    public boolean popPathFlow() {// no need
        pathFlow.pop();
        return true;
    }

    public Iterator<Link> getPathLink() {
        return pathLink.iterator();
    }


    public boolean pushPathLink(Link link) { // TODO - need CPY link manual?
        pathLink.push(link);
        return true;
    }


    public boolean popPathLink() {
        pathLink.pop();
        return true;
    }


    public boolean isPassDeviceId(DeviceId deviceId) {
        for (Link linkTemp : pathLink) {
            if (true == deviceId.equals(linkTemp.src().deviceId())) {
                return true;
            }
        }
        return false;
    }


    public PortCriterion getInport() {                       // TODO - check In_PORT or IN_PHY_PORT
        if (match.containsKey(Criterion.Type.IN_PORT)) {
            return (PortCriterion) match.get(Criterion.Type.IN_PORT);
        }
        return null;
    }


    /**
     * @param collision: as return value: if criteria contain mutiple criteria with same type, it is true
     * @return tsLoopPacket: if anyone is SetHeader_FAILURE, return null
     */
    public static tsLoopPacket matchBuilder(Iterable<Criterion> criteria, Return<Boolean> collision) {

        if (null != collision) {
            collision.setValue(false);
        }

        tsLoopPacket pkt = new tsLoopPacket();

        for (Criterion criterion : criteria) {
            if (null == pkt) {
                break;
            }

            int ret = pkt.setHeader(criterion);

            if (SetHeader_SUCCESS == ret) {

            } else if (SetHeader_OVERRIDE == ret) {
                if (null != collision) {
                    collision.setValue(true);
                }
            } else { // SetHeader_FAILURE
                pkt = null;
            }
        }

        return pkt;
    }

}
