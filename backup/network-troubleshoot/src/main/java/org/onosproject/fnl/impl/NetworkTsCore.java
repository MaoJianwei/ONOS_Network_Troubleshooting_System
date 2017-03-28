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
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.fnl.intf.NetWorkTsCheckLoopService;
import org.onosproject.fnl.intf.NetworkTsCoreService;
import org.onosproject.fnl.intf.NetworkTsFindBlackHoleService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * consideration - simply modularize at present.
 */
@Component(immediate = true)
@Service
public class NetworkTsCore implements NetworkTsCoreService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;
    private ApplicationId appId;
    // TODO - Attention - all modules has united AppId registered with united appName below


    /**
     * .
     */
    @Activate
    protected void activate() {
        log.info("Network Trouble-Shooting Core Init...");

        appId = coreService.registerApplication("org.onosproject.FNL.Network-Troubleshoot");

        log.info("Network Trouble-Shooting Core Started!");
    }

    /**
     * .
     */
    @Deactivate
    protected void deactivate() {
        log.info("Network Trouble-Shooting Core Stopped!");
    }


    @Override
    public void debug() {
    }

    @Override
    public List<TsLoopPacket> checkLoop() {
        NetWorkTsCheckLoopService loopService = DefaultServiceDirectory.getService(NetWorkTsCheckLoopService.class);
        return loopService.checkLoop();
    }

    @Override
    public List<TsFindBhPacket> findBlackHole(TrafficSelector trafficSelector) {
        NetworkTsFindBlackHoleService blackHoleService = DefaultServiceDirectory.
                getService(NetworkTsFindBlackHoleService.class);
        return blackHoleService.findBlackHole(trafficSelector);
    }


    // below is utility functions
    /**
     * Sort the flowEntry according to the flowEntry priority.
     *
     * @param flowEntries   the flowEntries to sort
     * @return the flowEntry in order
     */
    public static ArrayList<FlowEntry> sortFlowTable(Iterable<FlowEntry> flowEntries) {

        ArrayList<FlowEntry> flows = new ArrayList<>((HashSet<FlowEntry>) flowEntries);

        Collections.sort(flows,
                         (f1, f2) -> -(f1.priority() - f2.priority()));
        return flows;
    }
    /**
     * Sort the criterion according to the flowEntry type.
     *
     * @param criterionSet   the criterion to sort
     * @return the criterion in order
     */
    public static ArrayList<Criterion> sortCriteria(Set<Criterion> criterionSet) {

        ArrayList<Criterion> array = new ArrayList<>(criterionSet);
        Collections.sort(array,
                         (c1, c2) -> c1.type().compareTo(c2.type()));
        return array;
    }
    /**
     * Check if the connectPoint is a device point.
     *
     * @param connectPoint   the connectPoint to check
     * @return true when the connectPoint is a device point
     */
    public static boolean isDevice(ConnectPoint connectPoint) {
        return (connectPoint.elementId() instanceof DeviceId);
    }
    /**
     * Check if the connectPoint is a host point.
     *
     * @param connectPoint   the connectPoint to check
     * @return true when the connectPoint is a host point
     */
    public static boolean isHost(ConnectPoint connectPoint) { // TODO - not debug yet
        return (connectPoint.elementId() instanceof HostId);
    }
}
