/*
 * Copyright 2016 Open Networking Laboratory
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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Created by Janon on 12/19/15.
 */
public class TsFindBhPacket implements Serializable {

    private Map<Criterion.Type, Criterion> match;
    private Stack<FlowEntry> pathFlow;
    private Stack<DeviceId> pathDevice;
    private Stack<Link> pathLink;
    private Result result;
    private ConnectPoint srcLocation;
    private ConnectPoint dstLocation;

    TsFindBhPacket() {
        match = new HashMap<Criterion.Type, Criterion>();
        pathFlow = new Stack<FlowEntry>();
        pathDevice = new Stack<DeviceId>();
        pathLink = new Stack<Link>();
        srcLocation = new ConnectPoint(DeviceId.NONE, PortNumber.P0);
        dstLocation = new ConnectPoint(DeviceId.NONE, PortNumber.P0);
        result = Result.Default;
    }


    public SetHeaderValue setHeader(Criterion criterion) {
        boolean hasKey = match.containsKey(criterion.type());

        match.put(criterion.type(), criterion);

        return hasKey ? SetHeaderValue.SetHeader_OVERRIDE : SetHeaderValue.SetHeader_SUCCESS;
    }


    public Criterion getHeader(Criterion.Type criterionType) {
        return match.get(criterionType);
    }

    public boolean existHeader(Criterion.Type criterionType) {
        return match.containsKey(criterionType);
    }

    public boolean pushPathFlow(FlowEntry entry) {
        pathFlow.push(entry);
        return true;
    }



    public boolean pushPathDeviceId(DeviceId deviceId) {
        pathDevice.push(deviceId);
        return true;
    }


    public boolean pushPathLink(Link link) {
        pathLink.push(link);
        return true;
    }


    public boolean existDeviceId(DeviceId deviceId) {
        return pathDevice.contains(deviceId);
    }

    public Result getResult() {
        return result;
    }

    public boolean setResult(Result tmpresult) {
        result = tmpresult;
        return true;
    }

    public boolean setsrcLocation(ConnectPoint tmpsrcLocation) {
        srcLocation = tmpsrcLocation;
        return true;
    }

    public boolean setdstLocation(ConnectPoint tmpdstLocation) {
        dstLocation = tmpdstLocation;
        return true;
    }
    /**
     * Show the path the packet walk through when the packet reach the right destination.
     */
    public void showClearPath() {
        if (pathLink.isEmpty()) {
            System.out.print("The source host and the destination host are on the same device\n");
        } else {
            System.out.print("The path that the packet walk through is below:\n");
            System.out.print("Destination Host\n      |\n" + dstLocation.toString() + "\n      |\n");
            while (!pathLink.isEmpty()) {
                Link showLink = pathLink.pop();
                ConnectPoint showsrc = showLink.src();
                ConnectPoint showdst = showLink.dst();
                System.out.print(showdst.toString() + "\n      |\n" +
                                         showsrc.toString() + "\n      |\n");
            }
            System.out.print(srcLocation.toString() + "\n      |\nSource Host\n");
        }
    }
    /**
     * Show the path the packet walk through when the packet is blocked.
     */
    public void showBlockPath() {
        if (pathLink.isEmpty()) {
            System.out.print("The source host and the destination host are on the same device\n");
        } else {
            System.out.print("The path that the packet walk through is below:\nBlockPoint\n      |\n");
            while (!pathLink.isEmpty()) {
                Link showLink = pathLink.pop();
                ConnectPoint showsrc = showLink.src();
                ConnectPoint showdst = showLink.dst();
                System.out.print(showdst.toString() + "\n" + "      |\n" +
                                         showsrc.toString() + "\n      |\n");
            }
            System.out.print(srcLocation.toString() + "\n      |\nSource Host\n");
        }
    }
    /**
     * Construct a new object and copy the data from itself.
     *
     * @return the new object
     */
    public TsFindBhPacket copyBuild() {

        TsFindBhPacket newOne = new TsFindBhPacket();

        newOne.pathFlow = this.pathFlow;
        newOne.pathDevice = this.pathDevice;
        newOne.pathLink = this.pathLink;
        newOne.result = this.result;
        newOne.srcLocation = this.srcLocation;
        newOne.dstLocation = this.dstLocation;

        for (Map.Entry<Criterion.Type, Criterion> entry : this.match.entrySet()) {
            switch (entry.getKey()) {
                case IN_PORT:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchInPort(((PortCriterion) entry.getValue()).port()));
                    break;
                case ETH_SRC: // At present, not support Ethernet mask (ONOS?)
                    newOne.match.put(entry.getKey(), Criteria.
                            matchEthSrc(((EthCriterion) entry.getValue()).mac()));
                    break;
                case ETH_DST: // At present, not support Ethernet mask (ONOS?)
                    newOne.match.put(entry.getKey(), Criteria.
                            matchEthDst(((EthCriterion) entry.getValue()).mac()));
                    break;
                case ETH_TYPE:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchEthType(((EthTypeCriterion) entry.getValue()).ethType()));
                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS?)
                    newOne.match.put(entry.getKey(), Criteria.
                            matchVlanId(((VlanIdCriterion) entry.getValue()).vlanId()));
                    break;
                case VLAN_PCP:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchVlanPcp(((VlanPcpCriterion) entry.getValue()).priority()));
                    break;
                case IPV4_SRC:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchIPSrc(((IPCriterion) entry.getValue()).ip()));
                    break;
                case IPV4_DST:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchIPDst(((IPCriterion) entry.getValue()).ip()));
                    break;
                case IP_PROTO:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchIPProtocol(((IPProtocolCriterion) entry.getValue()).protocol()));
                    break;
                case IP_DSCP: // can't be supported by now
                    newOne.match.put(entry.getKey(), Criteria.
                            matchIPDscp(((IPDscpCriterion) entry.getValue()).ipDscp()));
                    break;
                case IP_ECN: // can't be supported by now
                    newOne.match.put(entry.getKey(), Criteria.
                            matchIPEcn(((IPEcnCriterion) entry.getValue()).ipEcn()));
                    break;
                case TCP_SRC:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchTcpSrc(((TcpPortCriterion) entry.getValue()).tcpPort()));
                    break;
                case TCP_DST:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchTcpDst(((TcpPortCriterion) entry.getValue()).tcpPort()));
                    break;
                case UDP_SRC:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchUdpSrc(((UdpPortCriterion) entry.getValue()).udpPort()));
                    break;
                case UDP_DST:
                    newOne.match.put(entry.getKey(), Criteria.
                            matchUdpDst(((UdpPortCriterion) entry.getValue()).udpPort()));
                    break;
                default:    //can't be supported by OF1.0
                    break;
            }
        }
        return newOne;
    }
    /**
     * Construct the criterion map.
     *
     * @param criterions   the criterions
     */
    public void matchBuilder(Iterable<Criterion> criterions) {

        for (Criterion criterion : criterions) {

            SetHeaderValue ret = this.setHeader(criterion);

            if (SetHeaderValue.SetHeader_SUCCESS == ret) {
                System.out.println(criterion.type() + " is set successfully");
            } else {
                System.out.println(criterion.type() + " is override successfully");
            }
        }

    }
    public enum Result {
        /**
         * Result initialization.
         */
        Default,
        /**
         * Can not find the specific host.
         */
        NOHOSTCONNECT,
        /**
         * Can not find a flow entry in flow table to match the packet.
         */
        NOFLOWENTRYMATCH,
        /**
         * The packet is transmitted into a loop route.
         */
        LOOP,
        /**
         * The packet arrives at the right host successfully.
         */
        ARRIVED,
        /**
         * The dstLink is empty.
         */
        NEXTLINKEMPTY,
        /**
         * The output port is not logical.
         */
        PORTNOTLOGICAL,
        /**
         * The packet arrives at a wrong dst host.
         */
        WRONGDSTHOST,
        /**
         * the dst point is not a device, which means that the link is the other type.
         */
        NOTDEVICE,
        /**
         * Output action does not exist in the instruction list or the output port is logical.
         */
        NOOUTPUTACTION,
        /**
         * The link is shut down.
         */
        LINKDOWN
    }

    enum SetHeaderValue {
        /**
         * Set Header successfully.
         */
        SetHeader_SUCCESS,
        /**
         * The header is override.
         */
        SetHeader_OVERRIDE
    }

}
