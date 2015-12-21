package org.onosproject.FNL;

import org.onlab.packet.EthType;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowEntry;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

/**
 * Created by mao on 12/5/15.
 */
public class TsLoopPacket implements Serializable {

    private Map<Criterion.Type, Criterion> match;
    private Stack<FlowEntry> pathFlow;
    private Stack<Link> pathLink; // TODO - Upgrade check - To make sure it include just Link but not EdgeLink

    private TsLoopPacket() {
        match = new HashMap<Criterion.Type, Criterion>();
        pathFlow = new Stack<FlowEntry>();
        pathLink = new Stack<Link>();
    }


    /**
     * TODO - cooperate - test pass 2012.12.21 15:54 by Mao
     * just copy match field, pathFlow and pathLink is Stack, carefully push and pop is making sense
     * @return
     */
    public TsLoopPacket copyPacketMatch() {

        TsLoopPacket newOne = new TsLoopPacket();

        newOne.pathFlow = this.pathFlow;
        newOne.pathLink = this.pathLink;

        for (Map.Entry<Criterion.Type, Criterion> entry : this.match.entrySet()) {
            switch (entry.getKey()) {
                case IN_PORT:
                    newOne.match.put(entry.getKey(), Criteria.matchInPort(((PortCriterion)entry.getValue()).port()));
                    break;
                case ETH_SRC: // At present, not support Ethernet mask (ONOS?)
                    newOne.match.put(entry.getKey(), Criteria.matchEthSrc(((EthCriterion)entry.getValue()).mac()));
                    break;
                case ETH_DST: // At present, not support Ethernet mask (ONOS?)
                    newOne.match.put(entry.getKey(), Criteria.matchEthDst(((EthCriterion)entry.getValue()).mac()));
                    break;
                case ETH_TYPE:
                    newOne.match.put(entry.getKey(), Criteria.matchEthType(((EthTypeCriterion)entry.getValue()).ethType()));
                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS?)
                    newOne.match.put(entry.getKey(), Criteria.matchVlanId(((VlanIdCriterion)entry.getValue()).vlanId()));
                    break;
                case VLAN_PCP:
                    newOne.match.put(entry.getKey(), Criteria.matchVlanPcp(((VlanPcpCriterion)entry.getValue()).priority()));
                    break;
                case IPV4_SRC:
                    newOne.match.put(entry.getKey(), Criteria.matchIPSrc(((IPCriterion)entry.getValue()).ip()));
                    break;
                case IPV4_DST:
                    newOne.match.put(entry.getKey(), Criteria.matchIPDst(((IPCriterion)entry.getValue()).ip()));
                    break;
                case IP_PROTO:
                    newOne.match.put(entry.getKey(), Criteria.matchIPProtocol(((IPProtocolCriterion)entry.getValue()).protocol()));
                    break;
                case IP_DSCP: // can't be supported by now
                    newOne.match.put(entry.getKey(), Criteria.matchIPDscp(((IPDscpCriterion)entry.getValue()).ipDscp()));
                    break;
                case IP_ECN: // can't be supported by now
                    newOne.match.put(entry.getKey(), Criteria.matchIPEcn(((IPEcnCriterion)entry.getValue()).ipEcn()));
                    break;
                case TCP_SRC:
                    newOne.match.put(entry.getKey(), Criteria.matchTcpSrc(((TcpPortCriterion)entry.getValue()).tcpPort()));
                    break;
                case TCP_DST:
                    newOne.match.put(entry.getKey(), Criteria.matchTcpDst(((TcpPortCriterion)entry.getValue()).tcpPort()));
                    break;
                case UDP_SRC:
                    newOne.match.put(entry.getKey(), Criteria.matchUdpSrc(((UdpPortCriterion)entry.getValue()).udpPort()));
                    break;
                case UDP_DST:
                    newOne.match.put(entry.getKey(), Criteria.matchUdpDst(((UdpPortCriterion)entry.getValue()).udpPort()));
                    break;
                default:    //can't be supported by OF1.0
                    break;
            }
        }
        return newOne;
    }

    @Deprecated
    public TsLoopPacket copyPacketSerial() {
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
        return (TsLoopPacket) ret;
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
    public static TsLoopPacket matchBuilder(Iterable<Criterion> criteria, Return<Boolean> collision) {

        if (null != collision) {
            collision.setValue(false);
        }

        TsLoopPacket pkt = new TsLoopPacket();

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
