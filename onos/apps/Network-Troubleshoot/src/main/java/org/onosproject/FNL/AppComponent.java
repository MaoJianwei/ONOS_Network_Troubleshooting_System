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
import org.onlab.packet.Ip4Prefix;
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
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
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


    //Loop variant start

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;


    //Loop variant end

    private void findLoop() {


        Set<DeviceId> excludeDeviceId = new HashSet<DeviceId>(); // DeviceId is OK?
        excludeDeviceId.clear();

        // Iterable<Device> networkDevice = deviceService.getDevices();
        Iterable<Host> hosts = hostService.getHosts();
        Set<Device> hostConnects = new HashSet<Device>();

        for (Host host : hosts) {
            hostConnects.add(deviceService.getDevice(host.location().deviceId()));
        }
        for (Device device : hostConnects) {

            if (excludeDeviceId.contains(device.id())) {
                continue;
            }

            Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(device.id()); // API: This will include flow rules which may not yet have been applied to the device.

            // TODO - sort flowtable

            for (FlowEntry flow : flowEntries) {
                tsLoopPacket pkt = tsLoopPacket.matchBuilder(flow.selector().criteria(), null);
                pkt.pushPathFlow(flow);

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

                                    if (isDevice(dstPoint)) {

                                        Device dstDevice = deviceService.getDevice(dstPoint.deviceId());
                                        pkt.pushPathDeviceId(dstPoint.deviceId());

                                        PortCriterion in_port = pkt.getInport();
                                        PortCriterion oldIn_port = in_port != null ? (PortCriterion) Criteria.matchInPort(in_port.port()) : null; // TODO - really copy this object

                                        pkt.setHeader(Criteria.matchInPort(dstPoint.port())); // new object

                                        tsLoopPacket newPkt = pkt;// TODO - COPY pkt

                                        boolean loopResult = lookup_flow(dstDevice, newPkt);
                                        if (true == loopResult) {
                                            // TODO
                                        }

                                        pkt.popPathDeviceId();

                                        pkt.setHeader(oldIn_port);


                                    } else {
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

        // TODO - return LOOP
    }

    /**
     * @param device
     * @param pkt    : when flows form a loop, pkt is also a return value indicating the loop header
     * @return
     */
    private boolean lookup_flow(Device device, tsLoopPacket pkt) {
        if (true == pkt.existDeviceId(device.id())) {
            return true; // TODO - Attention: pkt should be held outside
        }

        Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(device.id()); // API: This will include flow rules which may not yet have been applied to the device.

        // TODO - sort flowtable

        for (FlowEntry flowEntry : flowEntries) {

            Return<Boolean> isBigger = new Return<Boolean>();
            tsLoopPacket newPkt = pkt;// TODO - COPY pkt
            boolean matchResult = matchAndAdd_FlowEntry(flowEntry, newPkt, isBigger);
            if (false == matchResult) {
                continue;
            }

            newPkt.pushPathFlow(flowEntry); // drop this newPkt, and copy pkt again, no need to popPathFlow()

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

                                if (isDevice(dstPoint)) {

                                    Device dstDevice = deviceService.getDevice(dstPoint.deviceId());
                                    newPkt.pushPathDeviceId(dstPoint.deviceId());

                                    PortCriterion in_port = newPkt.getInport();
                                    PortCriterion oldIn_port = in_port != null ? (PortCriterion) Criteria.matchInPort(in_port.port()) : null; // TODO - really copy this object

                                    newPkt.setHeader(Criteria.matchInPort(dstPoint.port())); // new object

                                    tsLoopPacket newNewPkt = newPkt;// TODO - COPY pkt

                                    boolean loopResult = lookup_flow(dstDevice, newNewPkt);
                                    if (true == loopResult) {
                                        // TODO - check
                                        pkt = newNewPkt;
                                        return true;
                                    }

                                    newPkt.popPathDeviceId();

                                    newPkt.setHeader(oldIn_port);


                                } else {
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

            if (false == isBigger.getValue()) {
                break;
            } else {

            }
        }
        return false;
    }

    /**
     * @param flowEntry
     * @param pkt       will be updated for outside
     * @return match result
     */
    private boolean matchAndAdd_FlowEntry(FlowEntry flowEntry, tsLoopPacket pkt, Return<Boolean> isBigger) {

        isBigger.setValue(false);

        Set<Criterion> criterionSet = flowEntry.selector().criteria();
        for (Criterion criterion : criterionSet) {
            switch (criterion.type()) {
                case IN_PORT:

                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;

                case ETH_SRC:

                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }

                    break;
                case ETH_DST: // At present, not support Ethernet mask (ONOS)

                    break;
                case ETH_TYPE: // At present, not support Ethernet mask (ONOS)

                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS)
                    break;
                case VLAN_PCP:

                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }

                    break;
                case IPV4_SRC:

                    if (false == matchIPV4(pkt,criterion,isBigger))
                        return false;

                    break;
                case IPV4_DST:

                    if (false == matchIPV4(pkt,criterion,isBigger))
                        return false;

                    break;
                case IP_PROTO:
                    if (false == matchAddExactly(pkt, criterion, isBigger)) {
                        return false;
                    }
                    break;
                case IP_DSCP: // can't be supported by now
                    break;
                case IP_ECN: // can't be supported by now
                    break;
                case TCP_SRC: // TCP can't exist with UDP
                    if(true == pkt.existHeader(Criterion.Type.UDP_SRC) ||
                            true == pkt.existHeader(Criterion.Type.UDP_DST) ||
                            (pkt.existHeader(Criterion.Type.IP_PROTO) && ((IPProtocolCriterion)pkt.getHeader(Criterion.Type.IP_PROTO)).protocol() != 6)){
                        // has TCP match requirement, but can't afford TCP
                        return false;
                    }else{
                        // TODO - avoid IP_PROTO locates after TCP_* in this "for" loop
                        if (false == matchAddExactly(pkt, criterion, isBigger)) {
                            return false;
                        }
                    }
                    break;
                case TCP_DST: // TCP can't exist with UDP
                    break;
                case UDP_SRC: // UDP can't exist with TCP
                    break;
                case UDP_DST: // UDP can't exist with TCP
                    break;

                default:    //can't be supported by OF1.0
                    break;
            }

        }

        return true;
    }

    private boolean matchAddExactly(tsLoopPacket pkt, Criterion criterion, Return<Boolean> isBigger) {

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

    //TODO - check
    private boolean matchIPV4(tsLoopPacket pkt, Criterion criterion, Return<Boolean> isBigger){

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
            // TODO - check prerequisite
            pkt.setHeader(criterion);
            isBigger.setValue(true);
        }

        return true;
    }


    private boolean isDevice(ConnectPoint connectPoint) {        // TODO - not debug yet

        return (connectPoint.elementId() instanceof DeviceId);

//        if (connectPoint.elementId() instanceof DeviceId) {
//            return true;
//        }
//        return false;
    }

    private boolean isHost(ConnectPoint connectPoint) {          // TODO - not debug yet

        return (connectPoint.elementId() instanceof HostId);
//        if (connectPoint.elementId() instanceof HostId) {
//            return true;
//        }
//        return false;
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