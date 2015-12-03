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
package org.onosproject.FNL;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.NetworkTroubleshoot.NetworkTS;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeSet;

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
    public void debug(){
        findLoop();
    }


    //Loop variant start

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    //Loop variant end

    private void findLoop(){
        Set<String> excludeDeviceId = new TreeSet<String>(); // DeviceId is OK?
        excludeDeviceId.clear();

        Iterable<Device> networkDevice = deviceService.getDevices();
        Iterable<Host> hosts = hostService.getHosts();
        Set<Device> hostConnects = new TreeSet<Device>();

        for(Host host : hosts){
            hostConnects.add(deviceService.getDevice(host.location().deviceId()));
        }
        for (Device device : hostConnects){

            if(excludeDeviceId.contains(device.id().toString()))
                continue;

            Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(device.id()); // API: This will include flow rules which may not yet have been applied to the device.
            int a = 1;

        }

    }
}
