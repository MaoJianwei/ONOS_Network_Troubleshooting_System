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
package org.onosproject.fnl.intf;

import org.onosproject.fnl.impl.TsFindBhPacket;
import org.onosproject.net.flow.TrafficSelector;

import java.util.List;

/**
 * BlackHole Tracing Service, which is not designed to used as a Service in other apps.
 * We recommend you to use NetworkTsCoreService and invoke corresponding troubleshoot method.
 *
 * You can find recommended usage of this app by referring to TsDebugCommand class for detail.
 *
 * or get the NetworkTsCoreService by @Reference annotation.
 */
public interface NetworkTsFindBlackHoleService {

    /**
     * Enter of Blackhole tracking.
     *
     * @param trafficSelector the 12 tuples message
     * @return the result that contains the black hole location
     */
    List<TsFindBhPacket> findBlackHole(TrafficSelector trafficSelector);
}


