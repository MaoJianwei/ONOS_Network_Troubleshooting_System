/*
 * Copyright 2014 Open Networking Laboratory
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
/*
* protocol-specific by now, OpenFlow1.0
* 2015.12.05
 */
package org.onosproject.FNL;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.EthType;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkTroubleshoot.NetworkTS;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
@Service
public class AppComponent implements NetworkTS {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void debug() {
         findLoop();
    }


    //Loop Reference start

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    //Loop Reference end

    private List<TsLoopPacket> findLoop() {

        List<TsLoopPacket> loops = new ArrayList<TsLoopPacket>();
        Set<DeviceId> excludeDeviceId = new HashSet<DeviceId>(); // DeviceId is OK?

        Set<Device> hostConnects = new HashSet<Device>();
        Iterable<Host> hosts = hostService.getHosts();
        for (Host host : hosts) {
            hostConnects.add(deviceService.getDevice(host.location().deviceId()));
        }

        for (Device device : hostConnects) {

            if (excludeDeviceId.contains(device.id())) {
                continue;
            }

            // TODO - check - sort flowtable
            Iterable<FlowEntry> flowEntries = sortFlowTable(flowRuleService.getFlowEntries(device.id())); // API: This will include flow rules which may not yet have been applied to the device.

            for (FlowEntry flow : flowEntries) {

                TsLoopPacket pkt = TsLoopPacket.matchBuilder(flow.selector().criteria(), null);

                pkt.pushPathFlow(flow);
                //pkt.pushPathDeviceId(device.id());// obsolete // TO-DO - Attention!!! - Important!!! differ with me in RYU - need this push, because it pushes LINK in RYU, but DeviceId in ONOS here!

                List<Instruction> inst = flow.treatment().immediate();

                for (int i = 0; i < inst.size(); i++) {
                    switch (inst.get(i).type()) {
                        case L2MODIFICATION:
                            break;
                        case L3MODIFICATION:
                            break;
                        case L4MODIFICATION:
                            break;
                        case OUTPUT:

                            Instructions.OutputInstruction instOutPut = (Instructions.OutputInstruction) inst.get(i);

                            if (false == instOutPut.port().isLogical()) { // single OUTPUT - NIC or normal port

                                Set<Link> dstLink = linkService.getEgressLinks(new ConnectPoint(device.id(), instOutPut.port())); // TODO - debug

                                if (false == dstLink.isEmpty()) {

                                    Link dstThisLink = dstLink.iterator().next();
                                    ConnectPoint dstPoint = dstThisLink.dst(); // TODO - now, just deal with the first destination, will there be more destinations?

                                    if (true == isDevice(dstPoint)) {

                                        Device dstDevice = deviceService.getDevice(dstPoint.deviceId());

                                        pkt.pushPathLink(dstThisLink);

                                        PortCriterion in_port = pkt.getInport();
                                        PortCriterion oldIn_port = null != in_port ? (PortCriterion) Criteria.matchInPort(in_port.port()) : null; // TODO - check if it really copies this object

                                        pkt.setHeader(Criteria.matchInPort(dstPoint.port())); // new object

                                        TsLoopPacket newPkt = pkt.copyPacketMatch();// TODO - check - COPY pkt

                                        boolean loopResult = lookup_flow(dstDevice, newPkt);
                                        if (true == loopResult) {
                                            loops.add(newPkt);

                                            Iterator<Link> iter = newPkt.getPathLink();
                                            while (true == iter.hasNext()) {
                                                excludeDeviceId.add(iter.next().src().deviceId()); // false never mind
                                            }
                                        }

                                        pkt.popPathLink();

                                        pkt.setHeader(oldIn_port);


                                    } else { // Output to a Host
                                        ;
                                    }
                                } else {
                                    ;
                                }


                            } else if (instOutPut.port().equals(PortNumber.IN_PORT)) {

                            } else if (instOutPut.port().equals(PortNumber.NORMAL) ||
                                    instOutPut.port().equals(PortNumber.FLOOD) ||
                                    instOutPut.port().equals(PortNumber.ALL)) {

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
     * @param device
     * @param pkt    : when flows form a loop, pkt is also a return value indicating the loop header
     * @return
     */
    private boolean lookup_flow(Device device, TsLoopPacket pkt) {
        if (true == pkt.isPassDeviceId(device.id())) {
            return true; // Attention: pkt should be held outside
        }

        // TODO - check - sort flowtable
        Iterable<FlowEntry> flowEntries = sortFlowTable(flowRuleService.getFlowEntries(device.id())); // API: This will include flow rules which may not yet have been applied to the device.

        for (FlowEntry flowEntry : flowEntries) {

            Return<Boolean> isBigger = new Return<Boolean>();
            TsLoopPacket newPkt = pkt.copyPacketMatch();// TODO - check - COPY pkt
            boolean matchResult = matchAndAdd_FlowEntry(flowEntry, newPkt, isBigger);
            if (false == matchResult) {
                continue;
            }

            newPkt.pushPathFlow(flowEntry); // no need to popPathFlow(), because we will drop this newPkt, and copy pkt again,

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

                        if (false == instOutPut.port().isLogical()) { // single OUTPUT - NIC or normal port

                            Set<Link> dstLink = linkService.getEgressLinks(new ConnectPoint(device.id(), instOutPut.port())); // TODO - debug

                            if (false == dstLink.isEmpty()) {

                                Link dstThisLink = dstLink.iterator().next();
                                ConnectPoint dstPoint = dstThisLink.dst(); // TODO - now, just deal with the first destination, will there be more destinations?

                                if (true == isDevice(dstPoint)) {

                                    Device dstDevice = deviceService.getDevice(dstPoint.deviceId());
                                    newPkt.pushPathLink(dstThisLink);

                                    PortCriterion in_port = newPkt.getInport();
                                    PortCriterion oldIn_port = null != in_port ? (PortCriterion) Criteria.matchInPort(in_port.port()) : null; // TODO - check - if it really copies this object

                                    newPkt.setHeader(Criteria.matchInPort(dstPoint.port())); // new object

                                    TsLoopPacket newNewPkt = newPkt.copyPacketMatch();// TODO - check - COPY pkt

                                    boolean loopResult = lookup_flow(dstDevice, newNewPkt);
                                    if (true == loopResult) {
                                        pkt = newNewPkt; // TODO - check - it is able to return the new packet to find_loop()?
                                        return true;
                                    }

                                    newPkt.popPathLink();

                                    newPkt.setHeader(oldIn_port);


                                } else { // Output to a Host
                                    ;
                                }
                            } else {
                                ;
                            }


                        } else if (instOutPut.port().equals(PortNumber.IN_PORT)) {

                        } else if (instOutPut.port().equals(PortNumber.NORMAL) ||
                                instOutPut.port().equals(PortNumber.FLOOD) ||
                                instOutPut.port().equals(PortNumber.ALL)) {

                        }

                        break;
                    default://error!
                        break;
                }


            }

            newPkt.popPathFlow();

            if (false == isBigger.getValue()) {
                break;
            }
        }
        return false;
    }

    private ArrayList<FlowEntry> sortFlowTable(Iterable<FlowEntry> flowEntries) {

        ArrayList<FlowEntry> flows = new ArrayList<FlowEntry>((HashSet<FlowEntry>) flowEntries);

        Collections.sort(flows,
                         new Comparator<FlowEntry>() {
                             // TODO - sort timestamp
                             @Override
                             public int compare(FlowEntry f1, FlowEntry f2) {
                                 return -(f1.priority() - f2.priority());
                             }
                         });
        return flows;
    }

    private ArrayList<Criterion> sortCriteria(Set<Criterion> criterionSet) {

        ArrayList<Criterion> array = new ArrayList<Criterion>(criterionSet);
        Collections.sort(array,
                         new Comparator<Criterion>() {

                             @Override
                             public int compare(Criterion c1, Criterion c2) {
                                 return c1.type().compareTo(c2.type());

                             }
                         });
        return array;
    }

    private final int IP_PROTO_TCP_ts = 6;
    private final int IP_PROTO_UDP_ts = 17;

    /**
     * @param flowEntry
     * @param pkt       will be updated for outside
     * @return match result
     */
    private boolean matchAndAdd_FlowEntry(FlowEntry flowEntry, TsLoopPacket pkt, Return<Boolean> isBigger) {

        isBigger.setValue(false);

        ArrayList<Criterion> criterionArray = sortCriteria(flowEntry.selector().criteria());// TODO - check - sort criteria in order of packet headers //TODO- check - ERROR temperarily : Object[] can't tranform to Criterion[]

        for (Criterion criterion : criterionArray) {
            switch (criterion.type()) {
                case IN_PORT:// TODO - advance
                case ETH_SRC: // At present, not support Ethernet mask (ONOS?)
                case ETH_DST: // At present, not support Ethernet mask (ONOS?)
                case ETH_TYPE:
                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS?)
                case VLAN_PCP:
                    if (false == pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            false == ((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE)).ethType().equals(EthType.EtherType.VLAN)) {
                        return false;
                    }
                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IPV4_SRC:
                case IPV4_DST:
                    if (false == pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            false == ((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE)).ethType().equals(EthType.EtherType.IPV4)) {
                        return false;
                    }
                    if (false == matchAddIPV4(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_PROTO:
                    if (false == pkt.existHeader(Criterion.Type.ETH_TYPE) ||
                            false == ((EthTypeCriterion) pkt.getHeader(Criterion.Type.ETH_TYPE)).ethType().equals(EthType.EtherType.IPV4)) {
                        return false;
                    }
                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_DSCP: // can't be supported by now
                    break;
                case IP_ECN: // can't be supported by now
                    break;
                case TCP_SRC:
                case TCP_DST:
                    if (false == pkt.existHeader(Criterion.Type.IP_PROTO) ||
                            IP_PROTO_TCP_ts != ((IPProtocolCriterion) pkt.getHeader(Criterion.Type.IP_PROTO)).protocol()) {
                        return false; // has TCP match requirement, but can't afford TCP
                    }
                    // Done - avoid IP_PROTO locates after TCP_* in this "for" loop
                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case UDP_SRC:
                case UDP_DST:
                    if (false == pkt.existHeader(Criterion.Type.IP_PROTO) ||
                            IP_PROTO_UDP_ts != ((IPProtocolCriterion) pkt.getHeader(Criterion.Type.IP_PROTO)).protocol()) {
                        return false; // has UDP match requirement, but can't afford UDP
                    }
                    // Done - avoid IP_PROTO locates after UDP_* in this "for" loop
                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;

                default:    //can't be supported by OF1.0
                    break;
            }
        }
        return true;
    }

    private boolean matchAddExactly(TsLoopPacket pkt, Criterion criterion, Return<Boolean> isBigger) {

        if (true == pkt.existHeader(criterion.type())) {
            if (false == pkt.getHeader(criterion.type()).equals(criterion)) // Done - Checked - it will invoke the criterion's equal()
            {
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
     * before invoking me, must check prerequisite
     *
     * @param pkt
     * @param criterion
     * @param isBigger
     * @return
     */
    private boolean matchAddIPV4(TsLoopPacket pkt, Criterion criterion, Return<Boolean> isBigger) {//TODO - check

        if (true == pkt.existHeader(criterion.type())) {

            IpPrefix ipFlow = ((IPCriterion) criterion).ip();
            IpPrefix ipPkt = ((IPCriterion) pkt.getHeader(criterion.type())).ip();

            // attention - the order below is important
            if (true == ipFlow.equals(ipPkt)) {
                // shoot

            } else if (true == ipFlow.contains(ipPkt)) {
                // shoot, pkt is more exact than flowEntry

            } else if (true == ipPkt.contains(ipFlow)) {
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


    private boolean isDevice(ConnectPoint connectPoint) {        // TODO - not debug yet
        return (connectPoint.elementId() instanceof DeviceId);
    }

    private boolean isHost(ConnectPoint connectPoint) {          // TODO - not debug yet
        return (connectPoint.elementId() instanceof HostId);
    }
}


class Return<M> {

    private M ret = null;

    public void setValue(M value) {
        ret = value;
    }

    public M getValue() {
        return ret;
    }

    public boolean isNull() {
        return null == ret;
    }
}