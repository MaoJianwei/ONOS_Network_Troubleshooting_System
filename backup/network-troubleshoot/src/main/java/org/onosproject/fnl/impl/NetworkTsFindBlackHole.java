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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onosproject.fnl.intf.NetworkTsFindBlackHoleService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ONOS application component.
 * Function: find the black and fix them
 * created by Janon Wang
 */
@Component(immediate = false)
@Service
public class NetworkTsFindBlackHole implements NetworkTsFindBlackHoleService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * .
     */
    @Activate
    protected void activate() {
        log.info("Started");
    }

    /**
     * .
     */
    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private EdgePortService edgePortService;

    /**
     * .
     */
    private List<TsFindBhPacket> finalpkts;
    private Map<DeviceId, Iterable<FlowEntry>> flowcopy;

    private final int tcpTs = 6;
    private final int udpTs = 17;


    @Override
    public List<TsFindBhPacket> findBlackHole(TrafficSelector trafficSelector) {
        return startBlackHoleService(trafficSelector);
    }
    /**
     * Start finding the black hole.
     * Construct the inputPkt.
     *
     * @param trafficSelector   the 12 tuples message
     * @return the result that contains the black hole location
     */
    public List<TsFindBhPacket> startBlackHoleService(TrafficSelector trafficSelector) {

        finalpkts = new ArrayList<>();
        finalpkts.clear();

        //get all the flow entries information
        flowcopy = new HashMap<>();
        flowcopy.clear();
        Iterator<Device> allDevice = deviceService.getDevices().iterator();
        while (allDevice.hasNext()) {

            Device thisDevice = allDevice.next();
            flowcopy.put(thisDevice.id(), flowRuleService.getFlowEntries(thisDevice.id()));
        }

        Set<Criterion> inputCriterion = trafficSelector.criteria();


        InputPkt inputPkt = new InputPkt();
        for (Criterion criterion : inputCriterion) {
            inputPkt.setHeader(criterion);
        }

        EthCriterion src = (EthCriterion) inputPkt.getHeader(Criterion.Type.ETH_SRC);
        EthCriterion dst = (EthCriterion) inputPkt.getHeader(Criterion.Type.ETH_DST);
        Set<Host> srchosts = hostService.getHostsByMac(src.mac());
        Set<Host> dsthosts = hostService.getHostsByMac(dst.mac());


        if (srchosts.iterator().hasNext() && dsthosts.iterator().hasNext()) {
            Host srchost = srchosts.iterator().next();
            Host dsthost = dsthosts.iterator().next();
            inputPkt.setSrcHost(srchost);
            inputPkt.setDstHost(dsthost);

            //set the inportCriterion
            PortNumber inportNumber = srchost.location().port();
            PortCriterion inportCriterion = (PortCriterion) Criteria.matchInPort(inportNumber);
            inputPkt.setHeader(inportCriterion);
            return findBlackHole(inputPkt);
        } else {
            System.out.print("ERROR: the srchost or the dsthost is empty," +
                                     " please check the mac address you have input, or the edge link is down\n");
            return finalpkts;
        }

    }
    /**
     * Construct the TsFindBhPacket and check the source and destination host.
     *
     * @param inputPkt   Mapping the Criterion.Type and Criterion
     * @return the result that contains the black hole location
     */
    private List<TsFindBhPacket> findBlackHole(InputPkt inputPkt) {

        TsFindBhPacket matchpkt = new TsFindBhPacket();
        System.out.print("......Generate the TsFindBhPacket......\n");
        matchpkt.matchBuilder(inputPkt.criteria());

        Host host = inputPkt.getSrcHost();
        matchpkt.setsrcLocation(host.location());
        matchpkt.setdstLocation(inputPkt.getDstHost().location());
        Device hostconnect = deviceService.getDevice(host.location().deviceId());
        if (null == hostconnect) {
            matchpkt.setResult(TsFindBhPacket.Result.NOHOSTCONNECT);
            finalpkts.add(matchpkt);
            return finalpkts;
        }

        System.out.print("......Start locating the black hole......\n");
        boolean arrived = blackHoleRecursion(hostconnect, matchpkt);

        if (!arrived) {
            System.out.println("The packet is not arrived, black hole exists");
            return finalpkts;
        } else {
            System.out.println("The packet is arrived, there is no black hole");
            return finalpkts;
        }

    }
    /**
     * Using recursion to simulate the forwarding process.
     *
     * @param device   the next device
     * @param tmpmatchpkt   the forwarding packet
     * @return true when the packet is transmitted to the next jump
     */
    private boolean blackHoleRecursion(Device device, TsFindBhPacket tmpmatchpkt) {
        tmpmatchpkt.pushPathDeviceId(device.id());
        ArrayList<FlowEntry> flowEntries = NetworkTsCore.sortFlowTable(flowcopy.get(device.id()));
        FlowEntry matchflow = flowEntries.get(0);

        boolean ismatch = false;

        //find the match flow
        for (int i = 0; i < flowEntries.size(); i++) {

            FlowEntry flow = flowEntries.get(i);

            boolean matchResult = matchFlowEntry(flow, tmpmatchpkt);
            if (matchResult) {
                matchflow = flowEntries.get(i);
                ismatch = true;
                break;
            }
        }

        if (!ismatch) {
            tmpmatchpkt.setResult(TsFindBhPacket.Result.NOFLOWENTRYMATCH);
            finalpkts.add(tmpmatchpkt);
            return false;
        } else { //match a flow
            tmpmatchpkt.pushPathFlow(matchflow);

            List<Instruction> inst = matchflow.treatment().immediate();

            boolean isoutput = false; // if the pkt is output

            for (Instruction instone : inst) {
                switch (instone.type()) {
                    case L2MODIFICATION:
                        break;
                    case L3MODIFICATION:
                        break;
                    case L4MODIFICATION:
                        break;
                    case OUTPUT:
                        Instructions.OutputInstruction instOutPut = (Instructions.OutputInstruction) instone;

                        if (!instOutPut.port().isLogical()) {

                            ConnectPoint instConnectionPoint = new ConnectPoint(device.id(), instOutPut.port());

                            if (edgePortService.isEdgePoint(instConnectionPoint)) {
                                Set<Host> connectHosts = hostService.getConnectedHosts(instConnectionPoint);
                                if (connectHosts.isEmpty()) {
                                    tmpmatchpkt.setResult(TsFindBhPacket.Result.LINKDOWN);
                                    finalpkts.add(tmpmatchpkt);
                                    return false;
                                } else {
                                    isoutput = true;
                                    //get the dsthost
                                    EthCriterion ethCriterion = (EthCriterion) tmpmatchpkt.
                                            getHeader(Criterion.Type.ETH_DST);
                                    Set<Host> dsthosts = hostService.getHostsByMac(ethCriterion.mac());

                                    Host dsthost = dsthosts.iterator().next();
                                    //if the dsthost is the right dsthost by comparing two connection point
                                    //FIXME: if the dst edge link is down, it can not be detected
                                    if (dsthost.location().equals(instConnectionPoint)) {
                                        tmpmatchpkt.setResult(TsFindBhPacket.Result.ARRIVED);
                                        finalpkts.add(tmpmatchpkt);
                                        return true;
                                    } else { //the wrong dsthost
                                        tmpmatchpkt.setResult(TsFindBhPacket.Result.WRONGDSTHOST);
                                        finalpkts.add(tmpmatchpkt);
                                        return false;
                                    }

                                }

                            } else { //next jump is a device
                                //get the link from this device and filter them
                                Set<Link> allLink = linkService.getDeviceEgressLinks(device.id());
                                Set<Link> dstLinks = new HashSet<Link>();
                                //filter-- to find the right dstlink
                                Iterator<Link> it = allLink.iterator();
                                while (it.hasNext()) {
                                    Link dstLink = it.next();
                                    if (dstLink.src().port().equals(instOutPut.port())) {
                                        dstLinks.add(dstLink);
                                        break;
                                    }
                                }
                                if (!dstLinks.isEmpty()) {
                                    Link dstThisLink = dstLinks.iterator().next();
                                    tmpmatchpkt.pushPathLink(dstThisLink);
                                    ConnectPoint dstPoint = dstThisLink.dst();
                                        //Device
                                    if (NetworkTsCore.isDevice(dstPoint)) {
                                        isoutput = true; //connection point is exist for output

                                        Device dstDevice = deviceService.getDevice(dstPoint.deviceId());
                                        if (tmpmatchpkt.existDeviceId(dstDevice.id())) {
                                            tmpmatchpkt.setResult(TsFindBhPacket.Result.LOOP);
                                            finalpkts.add(tmpmatchpkt);
                                            return false;
                                        } else {
                                            //there may be many branches
                                            TsFindBhPacket newpkt = tmpmatchpkt.copyBuild();
                                            //change the inport
                                            PortNumber nextInPortNumber = dstThisLink.dst().port();
                                            PortCriterion nextInPortCriterion = (PortCriterion) Criteria.
                                                    matchInPort(nextInPortNumber);
                                            newpkt.setHeader(nextInPortCriterion);

                                            boolean recursionresult = blackHoleRecursion(dstDevice, newpkt);
                                            if (recursionresult) {
                                                return true;
                                            } else {
                                                continue;
                                            }
                                        }
                                    } else {
                                        tmpmatchpkt.setResult(TsFindBhPacket.Result.NOTDEVICE);
                                        return false;
                                    }
                                } else {
                                    tmpmatchpkt.setResult(TsFindBhPacket.Result.NEXTLINKEMPTY);
                                    return false;
                                }
                            }
                        } else {
                            tmpmatchpkt.setResult(TsFindBhPacket.Result.PORTNOTLOGICAL);
                            return false;
                        }
                        default:break;
                }

            }

            if (!isoutput) {
                tmpmatchpkt.setResult(TsFindBhPacket.Result.NOOUTPUTACTION);
                finalpkts.add(tmpmatchpkt);
                return false;
            } else {
                return false;
            }

        }

    }

    /**
     * Check if the flowEntry match the forwarding packet.
     *
     * @param flowEntry   the flowEntry get from the device
     * @param pkt   the forwarding packet
     * @return true when the flowEntry match the packet
     */
    private boolean matchFlowEntry(FlowEntry flowEntry, TsFindBhPacket pkt) {

        ArrayList<Criterion> criterionArray = NetworkTsCore.sortCriteria(flowEntry.selector().criteria());

        for (Criterion criterion : criterionArray) {
            switch (criterion.type()) {
                case IN_PORT:
                case ETH_SRC:
                case ETH_DST:
                case ETH_TYPE:
                    if (!matchExactly(pkt, criterion)) {
                        return false;
                    }
                    break;
                case VLAN_VID:
                case VLAN_PCP:
                    if (!pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE))
                                    .ethType().equals(EthType.EtherType.VLAN)) {
                        return false;
                    }
                    if (!matchExactly(pkt, criterion)) {
                        return false;
                    }
                    break;
                case IPV4_SRC:
                case IPV4_DST:
                    if (!pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE))
                                    .ethType().equals(EthType.EtherType.IPV4)) {
                        return false;
                    }
                    if (!matchAddIPV4(pkt, criterion)) {
                        return false;
                    }
                    break;
                case IP_PROTO:
                    if (!pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE))
                                    .ethType().equals(EthType.EtherType.IPV4)) {
                        return false;
                    }
                    if (!matchExactly(pkt, criterion)) {
                        return false;
                    }
                    break;
                case IP_DSCP:
                    break;
                case IP_ECN:
                    break;
                case TCP_SRC:
                case TCP_DST:
                    if (!pkt.existHeader(Criterion.Type.IP_PROTO) ||
                            tcpTs != ((IPProtocolCriterion) pkt.getHeader(Criterion.Type.IP_PROTO)).protocol()) {
                        return false;
                    }
                    if (!matchExactly(pkt, criterion)) {
                        return false;
                    }
                    break;
                case UDP_SRC:
                case UDP_DST:
                    if (!pkt.existHeader(Criterion.Type.IP_PROTO) ||
                            udpTs != ((IPProtocolCriterion) pkt.getHeader(Criterion.Type.IP_PROTO)).protocol()) {
                        return false;
                    }
                    if (!matchExactly(pkt, criterion)) {
                        return false;
                    }
                    break;

                default:
                    break;
            }
        }
        return true;
    }
    /**
     * Check if the criterion matching.
     *
     * @param pkt   the forwarding packet
     * @param criterion   the criterion
     * @return true when the criterion matched
     */
    private boolean matchExactly(TsFindBhPacket pkt, Criterion criterion) {
        return pkt.existHeader(criterion.type()) &&
                pkt.getHeader(criterion.type()).equals(criterion);
    }

    /**
     * .
     * @param pkt       :.
     * @param criterion :.
     * @return .
     */
    private boolean matchAddIPV4(TsFindBhPacket pkt, Criterion criterion) {
        if (pkt.existHeader(criterion.type())) {
            IpPrefix ipFlow = ((IPCriterion) criterion).ip();
            IpPrefix ipPkt = ((IPCriterion) pkt.getHeader(criterion.type())).ip();
            return ipFlow.equals(ipPkt) || ipFlow.contains(ipPkt);
        } else {
            return false;
        }
    }

    /**
     * .
     */
    private class InputPkt {
        private Map<Criterion.Type, Criterion> match;
        private Host srchost;
        private Host dsthost;

        /**
         * .
         */
        InputPkt() {
            match = new HashMap<>();
        }

        /**
         * .
         * @param tmphost   :.
         */
        public void setSrcHost(Host tmphost) {
            srchost = tmphost;
        }

        /**
         * .
         * @param tmphost   :.
         */
        public void setDstHost(Host tmphost) {
            dsthost = tmphost;
        }

        /**
         * .
         * @param criterion :.
         */
        public void setHeader(Criterion criterion) {
            match.put(criterion.type(), criterion);
        }

        /**
         * .
         * @param criterionType :.
         * @return .
         */
        public Criterion getHeader(Criterion.Type criterionType) {
            return match.get(criterionType);
        }

        /**
         * .
         * @return  :.
         */
        public Set<Criterion> criteria() {
        Set<Criterion> criterionSet = new HashSet<Criterion>();
            for (Criterion value :match.values()) {
            criterionSet.add(value);
            }
            return criterionSet;
        }

        /**
         * .
         * @return  :.
         */
        public Host getSrcHost() {
            return srchost;
        }

        /**
         * .
         * @return  :.
         */
        public Host getDstHost() {
            return dsthost;
        }
    }
}






