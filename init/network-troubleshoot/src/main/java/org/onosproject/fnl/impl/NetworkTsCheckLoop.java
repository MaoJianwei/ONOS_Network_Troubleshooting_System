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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onosproject.fnl.intf.NetWorkTsCheckLoopService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
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
import java.util.stream.Collectors;

/**
 * Loop Checking Service Impl, which is not designed to used as a Service in other apps.
 * We recommend you to use NetworkTsCoreService and invoke corresponding troubleshoot method.
 *
 * For example: get(NetworkTsCoreService.class).checkLoop()
 * or get the NetworkTsCoreService by @Reference annotation.
 */
@Component(immediate = false)
@Service
public class NetworkTsCheckLoop implements NetWorkTsCheckLoopService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private LinkService linkService;

    private ApplicationId appId;



    private static final int IP_PROTO_TCP_TS = 6;
    private static final int IP_PROTO_UDP_TS = 17;

    //except HostService
    private Map<DeviceId, Device> deviceInfo;
    private Map<DeviceId, Iterable<FlowEntry>> flowEntryInfo;
    private Map<DeviceId, Set<Link>> egressLinkInfo; //conventionally used by tsGetEgressLinks()


    @Activate
    public void activate() {

        appId = coreService.registerApplication("org.onosproject.FNL.Network-Troubleshoot");

        log.info("Network Trouble-Shooting Check Loop module is up!");
    }


    @Deactivate
    public void deactivate() {

        log.info("Network Trouble-Shooting Check Loop module clean up!");

    }


    @Override
    public List<TsLoopPacket> checkLoop() {

        return findLoop();
    }

    /**
     * Enter of the algorithm.
     * @return A List of {@link TsLoopPacket}, return empty list if there is no loop
     */
    private List<TsLoopPacket> findLoop() {

        getNetworkSnapshot();


        List<TsLoopPacket> loops = new ArrayList<>();
        Set<DeviceId> excludeDeviceId = new HashSet<>(); // DeviceId is OK?

        Set<Device> hostConnects = new HashSet<>();
        Iterable<Host> hosts = hostService.getHosts();
        for (Host host : hosts) {
            hostConnects.add(deviceInfo.get(host.location().deviceId()));
        }

        for (Device device : hostConnects) {

            if (excludeDeviceId.contains(device.id())) {
                continue;
            }

            Iterable<FlowEntry> flowEntries = NetworkTsCore.sortFlowTable(flowEntryInfo.get(device.id()));
            // API: This will include flow rules which may not yet have been applied to the device.

            for (FlowEntry flow : flowEntries) {

                TsLoopPacket pkt = TsLoopPacket.matchBuilder(flow.selector().criteria(), null);

                pkt.pushPathFlow(flow);

                List<Instruction> inst = flow.treatment().immediate();

                for (Instruction instOne : inst) {
                    switch (instOne.type()) {
                        case L2MODIFICATION:
                            break;
                        case L3MODIFICATION:
                            break;
                        case L4MODIFICATION:
                            break;
                        case OUTPUT:

                            Instructions.OutputInstruction instOutPut = (Instructions.OutputInstruction) instOne;

                            if (!instOutPut.port().isLogical()) { // single OUTPUT - NIC or normal port

                                Set<Link> dstLink = tsGetEgressLinks(new ConnectPoint(device.id(), instOutPut.port()));

                                if (!dstLink.isEmpty()) {

                                    Link dstThisLink = dstLink.iterator().next();
                                    ConnectPoint dstPoint = dstThisLink.dst();
                                    // TODO - now, just deal with the first destination,
                                    // will there be more destinations?

                                    if (NetworkTsCore.isDevice(dstPoint)) {

                                        Device dstDevice = deviceInfo.get(dstPoint.deviceId());

                                        pkt.pushPathLink(dstThisLink);

                                        PortCriterion inPort = pkt.getInport();
                                        PortCriterion oldInPort =
                                            null != inPort ? (PortCriterion) Criteria.matchInPort(inPort.port()) : null;
                                        // TODO - check if it really copies this object

                                        pkt.setHeader(Criteria.matchInPort(dstPoint.port())); // new object

                                        TsLoopPacket newPkt = pkt.copyPacketMatch();

                                        boolean loopResult = lookupFlow(dstDevice, newPkt);
                                        if (loopResult) {
                                            loops.add(newPkt);

                                            Iterator<Link> iter = newPkt.getPathLink();
                                            while (iter.hasNext()) {
                                                excludeDeviceId.add(iter.next().src().deviceId()); // false never mind
                                            }
                                        }

                                        pkt.resetLinkFlow(flow);

                                        if (oldInPort == null) {
                                            pkt.delHeader(Criterion.Type.IN_PORT);
                                        } else {
                                            pkt.setHeader(oldInPort);
                                        }


                                    } else { // Output to a Host

                                    }
                                }

                            } else if (instOutPut.port().equals(PortNumber.IN_PORT)) {
                                //TODO - in the future, we may need to resolve this condition 1
                            } else if (instOutPut.port().equals(PortNumber.NORMAL) ||
                                    instOutPut.port().equals(PortNumber.FLOOD) ||
                                    instOutPut.port().equals(PortNumber.ALL)) {
                                //TODO - in the future, we may need to resolve this condition 2
                            }

                            break;
                        default://error!
                            break;
                    }
                }
            }
        }

        // TODO - avoid two-hop LOOP

        // TODO - another clean operations

        return loops;
    }

    /**
     * Iterate one by one at switch hops.
     *
     * @param device : The Device needed to be check
     * @param pkt : when flows form a loop, pkt is also a return value indicating the loop header
     * @return : Whether we discover a Loop now or not. True for Loop discovery.
     */
    private boolean lookupFlow(Device device, TsLoopPacket pkt) {
        if (pkt.isPassDeviceId(device.id())) {
            return true; // Attention: pkt should be held outside
        }


        Iterable<FlowEntry> flowEntries = NetworkTsCore.sortFlowTable(flowEntryInfo.get(device.id()));
        // API: This will include flow rules which may not yet have been applied to the device.

        for (FlowEntry flowEntry : flowEntries) {

            TsReturn<Boolean> isBigger = new TsReturn<>();
            TsLoopPacket newPkt = pkt.copyPacketMatch();
            boolean matchResult = matchAndAddFlowEntry(flowEntry, newPkt, isBigger);
            if (!matchResult) {
                continue;
            }

            newPkt.pushPathFlow(flowEntry);
            // no need to popPathFlow(), because we will drop this newPkt, and copy pkt again,

            List<Instruction> inst = flowEntry.treatment().immediate();

            for (Instruction instOne : inst) {

                switch (instOne.type()) {
                    case L2MODIFICATION:
                        break;
                    case L3MODIFICATION:
                        break;
                    case L4MODIFICATION:
                        break;
                    case OUTPUT:

                        Instructions.OutputInstruction instOutPut = (Instructions.OutputInstruction) instOne;

                        if (!instOutPut.port().isLogical()) { // single OUTPUT - NIC or normal port

                            Set<Link> dstLink = tsGetEgressLinks(new ConnectPoint(device.id(), instOutPut.port()));

                            if (!dstLink.isEmpty()) {

                                Link dstThisLink = dstLink.iterator().next();
                                ConnectPoint dstPoint = dstThisLink.dst();
                                // TODO - now, just deal with the first destination, will there be more destinations?

                                if (NetworkTsCore.isDevice(dstPoint)) {

                                    Device dstDevice = deviceInfo.get(dstPoint.deviceId());
                                    newPkt.pushPathLink(dstThisLink);

                                    PortCriterion inPort = newPkt.getInport();
                                    PortCriterion oldInPort =
                                            null != inPort ? (PortCriterion) Criteria.matchInPort(inPort.port()) : null;
                                    // TODO - check - if it really copies this object

                                    newPkt.setHeader(Criteria.matchInPort(dstPoint.port())); // new object

                                    TsLoopPacket newNewPkt = newPkt.copyPacketMatch();

                                    boolean loopResult = lookupFlow(dstDevice, newNewPkt);
                                    if (loopResult) {
                                        pkt.handInLoopMatch(newNewPkt);
                                        return true;
                                    }

                                    newPkt.popPathLink();

                                    if (oldInPort == null) {
                                        pkt.delHeader(Criterion.Type.IN_PORT);
                                    } else {
                                        pkt.setHeader(oldInPort);
                                    }


                                } else { // Output to a Host

                                }
                            } else {
                                //TODO - NEED
                            }


                        } else if (instOutPut.port().equals(PortNumber.IN_PORT)) {
                            //TODO - in the future, we may need to resolve this condition 1
                        } else if (instOutPut.port().equals(PortNumber.NORMAL) ||
                                instOutPut.port().equals(PortNumber.FLOOD) ||
                                instOutPut.port().equals(PortNumber.ALL)) {
                            //TODO - in the future, we may need to resolve this condition 2
                        }

                        break;
                    default://error!
                        break;
                }


            }

            newPkt.popPathFlow();

            if (!isBigger.getValue()) {
                break;
            }
        }
        return false;
    }


    private void getNetworkSnapshot() {
        deviceInfo = new HashMap<>();
        Iterable<Device> deviceTemp = deviceService.getDevices();
        for (Device d : deviceTemp) {
            deviceInfo.put(d.id(), d);
        }

        flowEntryInfo = new HashMap<>();
        for (Map.Entry<DeviceId, Device> d : deviceInfo.entrySet()) {
            flowEntryInfo.put(d.getKey(), flowRuleService.getFlowEntries(d.getKey()));
        }

        egressLinkInfo = new HashMap<>();
        for (Map.Entry<DeviceId, Device> d : deviceInfo.entrySet()) {
            egressLinkInfo.put(d.getKey(), linkService.getDeviceEgressLinks(d.getKey()));
        }

    }

    private Set<Link> tsGetEgressLinks(ConnectPoint point) {
        Set<Link> portEgressLink = new HashSet<>();

        DeviceId deviceId = point.deviceId();

        Set<Link> allEgressLink = egressLinkInfo.get(deviceId);

        portEgressLink.addAll(
                allEgressLink
                        .stream()
                        .filter(l->l.src().equals(point))
                        .collect(Collectors.toList()));

        return portEgressLink;
    }

    private boolean matchAndAddFlowEntry(FlowEntry flowEntry, TsLoopPacket pkt, TsReturn<Boolean> isBigger) {

        isBigger.setValue(false);

        ArrayList<Criterion> criterionArray = NetworkTsCore.sortCriteria(flowEntry.selector().criteria());

        for (Criterion criterion : criterionArray) {
            switch (criterion.type()) {
                case IN_PORT:// TODO - advance
                case ETH_SRC: // At present, not support Ethernet mask (ONOS?)
                case ETH_DST: // At present, not support Ethernet mask (ONOS?)
                case ETH_TYPE:
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS?)
                case VLAN_PCP:
                    if (!pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE))
                                    .ethType().equals(EthType.EtherType.VLAN.ethType())) {
                        return false;
                    }
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IPV4_SRC:
                case IPV4_DST:
                    if (!pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE))
                                    .ethType().equals(EthType.EtherType.IPV4.ethType())) {
                        return false;
                    }
                    if (!matchAddIPV4(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_PROTO:
                    if (!pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            !((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE))
                                    .ethType().equals(EthType.EtherType.IPV4.ethType())) {
                        return false;
                    }
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_DSCP: // can't be supported by now
                    break;
                case IP_ECN: // can't be supported by now
                    break;
                case TCP_SRC:
                case TCP_DST:
                    if (!pkt.existHeader(Criterion.Type.IP_PROTO) ||
                            IP_PROTO_TCP_TS !=
                                    ((IPProtocolCriterion) pkt.getHeader(Criterion.Type.IP_PROTO)).protocol()) {
                        return false; // has TCP match requirement, but can't afford TCP
                    }
                    // Done - avoid IP_PROTO locates after TCP_* in this "for" loop
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case UDP_SRC:
                case UDP_DST:
                    if (!pkt.existHeader(Criterion.Type.IP_PROTO) ||
                            IP_PROTO_UDP_TS !=
                                    ((IPProtocolCriterion) pkt.getHeader(Criterion.Type.IP_PROTO)).protocol()) {
                        return false; // has UDP match requirement, but can't afford UDP
                    }
                    // Done - avoid IP_PROTO locates after UDP_* in this "for" loop
                    if (!matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;

                default:    //can't be supported by OF1.0
                    break;
            }
        }
        return true;
    }

    private boolean matchAddExactly(TsLoopPacket pkt, Criterion criterion, TsReturn<Boolean> isBigger) {

        if (pkt.existHeader(criterion.type())) {
            // Done - Checked - it will invoke the criterion's equal()
            if (!pkt.getHeader(criterion.type()).equals(criterion)) {
                return false;
            }

        } else {
            // TODO - check if it is IN_PORT or IN_PHY_PORT, should be strict
            pkt.setHeader(criterion);
            isBigger.setValue(true);
        }

        return true; // should put it here
    }

    /**
     * before invoking me, must check prerequisite.
     */
    private boolean matchAddIPV4(TsLoopPacket pkt, Criterion criterion, TsReturn<Boolean> isBigger) {

        if (pkt.existHeader(criterion.type())) {

            IpPrefix ipFlow = ((IPCriterion) criterion).ip();
            IpPrefix ipPkt = ((IPCriterion) pkt.getHeader(criterion.type())).ip();

            // attention - the order below is important
            if (ipFlow.equals(ipPkt)) {
                // shoot

            } else if (ipFlow.contains(ipPkt)) {
                // shoot, pkt is more exact than flowEntry

            } else if (ipPkt.contains(ipFlow)) {
                // pkt should be changed to be more exact
                pkt.setHeader(criterion);
                isBigger.setValue(true);
            } else {
                // match fail
                return false;
            }

        } else {
            // Done - no need - check prerequisite - attention the order of criteria in "for" loop
            pkt.setHeader(criterion);
            isBigger.setValue(true);
        }

        return true;
    }

}
