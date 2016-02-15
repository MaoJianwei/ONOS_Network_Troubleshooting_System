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
package org.onosproject.fnl.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.fnl.impl.TsFindBhPacket;
import org.onosproject.fnl.intf.NetworkTsCoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import java.util.List;


/**
 * A service that find the black holes and fix them.
 * create by Janon Wang
 */
@Command(scope = "onos", name = "findblackhole",
         description = "find the black holes and fix them")

public class TsFindBlackHole extends AbstractShellCommand {

    @Argument(index = 0, name = "headFields", description = "EthSrcMac, EthDstMac, EthType, VlanId, " +
            "VlanPcp, IpSrc, IpDst, Ipproto, IpToS, TcpSrc, TcpDst ",
              required = true, multiValued = false)
    private String headFields = null;
    private final int headlength = 11;

    @Override
    protected void execute() {
        //get a reference to our service(the key point to deliver the parameter)
        NetworkTsCoreService service = get(NetworkTsCoreService.class);

        TrafficSelector.Builder trafficSelectorBuild = DefaultTrafficSelector.builder();
        String[] headers = headFields.split(",");

        if (headlength != headers.length) {
            System.out.print("input error!");
            return;
        }

        if (!headers[0].isEmpty()) {
            trafficSelectorBuild.matchEthSrc(MacAddress.valueOf(headers[0]));
            print("the EthSrcMac is set successfully");
        }
        if (!headers[1].isEmpty()) {
            trafficSelectorBuild.matchEthDst(MacAddress.valueOf(headers[1]));
            print("the EthDstMac is set successfully");
        }
        if (!headers[2].isEmpty()) {
            short ethType = (short) Integer.parseInt(headers[2], 16);
            trafficSelectorBuild.matchEthType(ethType);
            print("the EthType is set successfully");
        }
        if (!headers[3].isEmpty()) {
            short value = (short) Integer.parseInt(headers[3], 16);
            VlanId vlanId = VlanId.vlanId(value);
            trafficSelectorBuild.matchVlanId(vlanId);
            print("the VlanId is set successfully");
        }
        if (!headers[4].isEmpty()) {
            byte value = uniteBytes(headers[4]);
            trafficSelectorBuild.matchVlanPcp(value);
            print("the VlanPcp is set successfully");
        }
        if (!headers[5].isEmpty()) {
            trafficSelectorBuild.matchIPSrc(IpPrefix.valueOf(headers[5]));
            print("the IpSrc is set successfully");
        }
        if (!headers[6].isEmpty()) {
            trafficSelectorBuild.matchIPDst(IpPrefix.valueOf(headers[6]));
            print("the IpDst is set successfully");
        }
        if (!headers[7].isEmpty()) {
            byte value = uniteBytes(headers[7]);
            trafficSelectorBuild.matchIPProtocol(value);
            print("the IpProto is set successfully");
        }
        if (!headers[8].isEmpty()) {
            byte value = uniteBytes(headers[8]);
            trafficSelectorBuild.matchIPEcn(value);
            print("the IpToS is set successfully");
        }
        if (!headers[9].isEmpty()) {
            Integer value = Integer.parseInt(headers[9]);
            trafficSelectorBuild.matchTcpSrc(TpPort.tpPort(value));
            print("the TcpSrc is set successfully");
        }
        if (!headers[10].isEmpty()) {
            Integer value = Integer.parseInt(headers[10]);
            trafficSelectorBuild.matchTcpDst(TpPort.tpPort(value));
            print("the TcpDst is set successfully");
        }
        TrafficSelector trafficSelector = trafficSelectorBuild.build();

        List<TsFindBhPacket> findBlackHoleresult = service.findBlackHole(trafficSelector);

        if (findBlackHoleresult.isEmpty()) {
            return;
        }
        System.out.print("......End locating, show the result......\n");
        for (TsFindBhPacket tmppkt : findBlackHoleresult) {
            switch (tmppkt.getResult()) {
                //TODO: we will fix the black hole according to different situation in the next vision
                case Default:
                    System.out.println("Situation:DEFAULT");
                    break;
                case NOHOSTCONNECT:
                    System.out.println("Situation:NOHOSTCONNECT");
                    break;
                case NOFLOWENTRYMATCH:
                    System.out.println("Situation:NOFLOWENTRYMATCH");
                    tmppkt.showBlockPath();
                    break;
                case LOOP:
                    System.out.println("Situation:LOOP");
                    tmppkt.showBlockPath();
                    break;
                case ARRIVED:
                    System.out.println("Situation:ARRIVED");
                    tmppkt.showClearPath();
                    break;
                case NEXTLINKEMPTY:
                    System.out.println("Situation:DSTLINKEMPTY");
                    tmppkt.showBlockPath();
                    break;
                case NOTDEVICE:
                    System.out.println("Situation:NOTDEVICE");
                    tmppkt.showBlockPath();
                    break;
                case PORTNOTLOGICAL:
                    System.out.println("Situation:PORTNOTLOGICAL");
                    tmppkt.showBlockPath();
                    break;
                case NOOUTPUTACTION:
                    System.out.println("Situation:NOOUTPUTACTION");
                    tmppkt.showBlockPath();
                    break;
                case WRONGDSTHOST:
                    System.out.println("Situation:WRONGDSTHOST");
                    tmppkt.showBlockPath();
                    break;
                case LINKDOWN:
                    System.out.println("Situation:LINKDOWN");
                    tmppkt.showBlockPath();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Synthesizing a byte from two ASCII characters.
     * example: "EF"--> 0xEF
     * @param strings string
     * @return byte
     */
    private byte uniteBytes(String strings) {
        byte[] src = strings.getBytes();
        byte src0 = src[0];
        byte src1 = src[1];
        byte b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        b0 = (byte) (b0 << 4);
        byte b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        return (byte) (b0 ^ b1);
    }
}

