/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.fnl.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * Virtual packet for Loop Checking.
 */
public final class TsLoopPacket {

    private Map<Criterion.Type, Criterion> match;
    private Stack<FlowEntry> pathFlow;
    private Stack<Link> pathLink; // TODO - Upgrade check - To make sure it include just Link but not EdgeLink

    /**
     * Create an initial virtual packet inside for Loop Checking.
     */
    private TsLoopPacket() {
        match = new HashMap<Criterion.Type, Criterion>();
        pathFlow = new Stack<FlowEntry>();
        pathLink = new Stack<Link>();
    }


    /**
     * Just copy match field, pathFlow and pathLink is Stack, carefully push and pop is making sense.
     *
     * @return New {@link TsLoopPacket} object mainly with my match
     */
    public TsLoopPacket copyPacketMatch() {

        TsLoopPacket newOne = new TsLoopPacket();

        newOne.pathFlow = this.pathFlow;
        newOne.pathLink = this.pathLink;

        for (Map.Entry<Criterion.Type, Criterion> entry : this.match.entrySet()) {
            switch (entry.getKey()) {
                case IN_PORT:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchInPort(((PortCriterion) entry.getValue()).port()));
                    break;
                case ETH_SRC: // At present, not support Ethernet mask (ONOS?)
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchEthSrc(((EthCriterion) entry.getValue()).mac()));
                    break;
                case ETH_DST: // At present, not support Ethernet mask (ONOS?)
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchEthDst(((EthCriterion) entry.getValue()).mac()));
                    break;
                case ETH_TYPE:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchEthType(((EthTypeCriterion) entry.getValue()).ethType()));
                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS?)
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchVlanId(((VlanIdCriterion) entry.getValue()).vlanId()));
                    break;
                case VLAN_PCP:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchVlanPcp(((VlanPcpCriterion) entry.getValue()).priority()));
                    break;
                case IPV4_SRC:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchIPSrc(((IPCriterion) entry.getValue()).ip()));
                    break;
                case IPV4_DST:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchIPDst(((IPCriterion) entry.getValue()).ip()));
                    break;
                case IP_PROTO:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchIPProtocol(((IPProtocolCriterion) entry.getValue()).protocol()));
                    break;
                case IP_DSCP: // can't be supported by now
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchIPDscp(((IPDscpCriterion) entry.getValue()).ipDscp()));
                    break;
                case IP_ECN: // can't be supported by now
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchIPEcn(((IPEcnCriterion) entry.getValue()).ipEcn()));
                    break;
                case TCP_SRC:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchTcpSrc(((TcpPortCriterion) entry.getValue()).tcpPort()));
                    break;
                case TCP_DST:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchTcpDst(((TcpPortCriterion) entry.getValue()).tcpPort()));
                    break;
                case UDP_SRC:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchUdpSrc(((UdpPortCriterion) entry.getValue()).udpPort()));
                    break;
                case UDP_DST:
                    newOne.match.put(entry.getKey(),
                                     Criteria.matchUdpDst(((UdpPortCriterion) entry.getValue()).udpPort()));
                    break;
                default:    //can't be supported by OF1.0
                    break;
            }
        }
        return newOne;
    }


    /**
     * Set header successfully.
     */
    public static final int SETHEADER_SUCCESS = 40123;
    /**
     * Set header successfully but override old value.
     */
    public static final int SETHEADER_OVERRIDE = 40110;
    /**
     * Fail to set Header because NULL value.
     */
    public static final int SETHEADER_FAILURE_NULL = 7181;
    /**
     * Fail to set Header, but reason is not defined, defined in advance.
     */
    public static final int SETHEADER_FAILURE = 5511;


    /**
     * Set a packet header field.
     * @param criterion {@link Criterion} as packet header field
     * @return The result of set action
     */
    public int setHeader(Criterion criterion) {

        if (criterion == null) {
            return SETHEADER_FAILURE_NULL;
        }

        boolean hasKey = match.containsKey(criterion.type());

        match.put(criterion.type(), criterion);

        return hasKey ? SETHEADER_OVERRIDE : SETHEADER_SUCCESS;
    }

    /**
     * Delete a packet header field by designating header type.
     * @param criterionType {@link Criterion.Type} as packet header type
     * @return The result of delete action
     */
    public boolean delHeader(Criterion.Type criterionType) {
        if (!(match.containsKey(criterionType))) {
            return false;
        } else {
            match.remove(criterionType);
            return true;
        }
    }

    /**
     * Delete a packet header field value by designating header type.
     * @param criterionType {@link Criterion.Type} as packet header type
     * @return the packet header field value;
     *         {@code null} if the field do not exist.
     */
    public Criterion getHeader(Criterion.Type criterionType) {
        return match.getOrDefault(criterionType, null);
    }

    /**
     * Judge if there is the corresponding type of header field in the packet.
     * @param criterionType {@link Criterion.Type} as packet header type
     * @return {@code true} if the field exists;
     *         {@code false} if not.
     */
    public boolean existHeader(Criterion.Type criterionType) {
        return match.containsKey(criterionType);
    }

    /**
     * Add a {@link FlowEntry} to path flowEntry list.
     * Packet match this entry in specific switch hop.
     *
     * @param entry The matched entry.
     * @return {@true} if add action is successful.
     */
    public boolean pushPathFlow(FlowEntry entry) {
        pathFlow.push(entry);
        return true;
    }

    /**
     * Delete a {@link FlowEntry} from path flowEntry list.
     * @return {@true} if delete action is successful.
     */
    public boolean popPathFlow() {
        //we may have no need
        pathFlow.pop();
        return true;
    }

    /**
     * Get links in the path which the packet pass through.
     * @return an {@link Iterator<Link>} object.
     */
    public Iterator<Link> getPathLink() {
        return pathLink.iterator();
    }

    /**
     * Add a {@link Link} to path link list.
     * Packet go through this link between two switches.
     *
     * @param link The link through which the packet go.
     * @return {@true} if add action is successful.
     */
    public boolean pushPathLink(Link link) { // TODO - need CPY link manual?
        pathLink.push(link);
        return true;
    }

    /**
     * Delete a {@link Link} from path link list.
     * @return {@true} if delete action is successful.
     */
    public boolean popPathLink() {
        pathLink.pop();
        return true;
    }

    /**
     * Judge whether the packet passed the device or not.
     * @param deviceId The device to be judge.
     * @return {@true} if packet passed the specific switch.
     *         {@false} if not.
     */
    public boolean isPassDeviceId(DeviceId deviceId) {
        for (Link linkTemp : pathLink) {
            if (deviceId.equals(linkTemp.src().deviceId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the IN_PORT header field of the packet.
     * Attention: it will be change when packet go into a switch hop.
     * @return a {@link PortCriterion} object.
     */
    public PortCriterion getInport() {
        // TODO - check IN_PORT or IN_PHY_PORT
        if (match.containsKey(Criterion.Type.IN_PORT)) {
            return (PortCriterion) match.get(Criterion.Type.IN_PORT);
        }
        return null;
    }

    /**
     * Create a {@link TsLoopPacket} object with given Match Fields {@link Iterable<Criterion>}.
     * @param criteria  : Match field of one flow entry.
     * @param collision : As return value: if criteria contain mutiple criteria with same type, it is true
     * @return tsLoopPacket: if anyone is SetHeader_FAILURE, return null
     */
    public static TsLoopPacket matchBuilder(Iterable<Criterion> criteria, TsReturn<Boolean> collision) {

        if (null != collision) {
            collision.setValue(false);
        }

        TsLoopPacket pkt = new TsLoopPacket();

        for (Criterion criterion : criteria) {
            if (null == pkt) {
                break;
            }

            int ret = pkt.setHeader(criterion);

            if (SETHEADER_SUCCESS == ret) {
                //TODO - in the future, we may need to resolve this condition
            } else if (SETHEADER_OVERRIDE == ret) {
                if (null != collision) {
                    collision.setValue(true);
                }
            } else { // SetHeader_FAILURE  or SetHeader_FAILURE_NULL
                pkt = null;
            }
        }

        return pkt;
    }

    /**
     * When a Loop is discovered, hand in the header of virtual packet one by one.
     * @param loopPkt {@link TsLoopPacket} object,
     *                which includes the header of virtual packet which will triggers Loop Storm.
     */
    public void handInLoopMatch(TsLoopPacket loopPkt) {
        this.match = loopPkt.match;
    }

    /**
     * Clear and init pathLink and pathFlow.
     * @param firstEntry The flow entry from which this packet is built.
     */
    public void resetLinkFlow(FlowEntry firstEntry) {
        pathLink = new Stack<Link>();
        pathFlow = new Stack<FlowEntry>();
        pathFlow.push(firstEntry);
    }


    @Override
    public String toString() {
        StringBuilder me = new StringBuilder();

        me.append("\n================================================================\n");

        me.append("\n---------- Loop Header ---------\n");

        ArrayList<Criterion> criteria = new ArrayList(match.values());
        Collections.sort(criteria,
                         (o1, o2)-> o1.type().compareTo(o2.type()));

        for (Criterion c : criteria) {
            me.append(c.toString()).append("\n");
        }

        me.append("\n------- Loop FlowEntries -------\n");

        for (FlowEntry flow:pathFlow) {
            me.append(flow.toString()).append("\n");
        }

        me.append("\n---------- Loop Links ----------\n");

        for (Link l:pathLink) {
            me.append(l.toString()).append("\n");
        }

        return me.toString();
    }
}
