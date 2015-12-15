package org.onosproject.FNL;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;

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

    public Map<Criterion.Type, Criterion> match;
    public Stack<FlowEntry> pathFlow;
    public Stack<DeviceId> pathDevice;

    tsLoopPacket() {
        match = new HashMap<Criterion.Type, Criterion>();
        pathFlow = new Stack<FlowEntry>();
        pathDevice = new Stack<DeviceId>();
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

    ;

    public Criterion getHeader(Criterion.Type criterionType) {
        return match.get(criterionType); // TODO - check is null when the key is not contained?
    }

    public boolean existHeader(Criterion.Type criterionType) {
        return match.containsKey(criterionType);
    }

    ;


    public boolean pushPathFlow(FlowEntry entry) {
        pathFlow.push(entry);
        return true;
    }

    ;

    public boolean popPathFlow() {// no need
        pathFlow.pop();
        return true;
    }

    ;

    public boolean pushPathDeviceId(DeviceId deviceId) {
        pathDevice.push(deviceId);
        return true;
    }

    ;

    public boolean popPathDeviceId() {
        pathDevice.pop();
        return true;
    }

    ;

    public boolean existDeviceId(DeviceId deviceId) {
        return pathDevice.contains(deviceId);
    }

    ;

    public PortCriterion getInport() {                       // TODO - check In_PORT or IN_PHY_PORT
        if (match.containsKey(Criterion.Type.IN_PORT)) {
            return (PortCriterion) match.get(Criterion.Type.IN_PORT);
        }
        return null;
    }

    public tsLoopPacket copyBuild() {
        return this; // TODO
    }


    /**
     * @param collision: as return value: if criterion contain mutiple criterion with same type, it is true
     * @return tsLoopPacket: if anyone is SetHeader_FAILURE, return null
     */
    public static tsLoopPacket matchBuilder(Iterable<Criterion> criterion, Boolean collision) {

        if (null != collision) {
            collision = true;
        }

        tsLoopPacket pkt = new tsLoopPacket();

        for (Criterion criteria : criterion) {
            if (null == pkt) {
                break;
            }

            switch (pkt.setHeader(criteria)) {
                case SetHeader_SUCCESS:
                    break;
                case SetHeader_OVERRIDE:
                    if (null != collision) {
                        collision = false;
                    }
                    break;
                case SetHeader_FAILURE:
                    pkt = null;
                    break;
            }
        }

        return pkt;
    }

}
