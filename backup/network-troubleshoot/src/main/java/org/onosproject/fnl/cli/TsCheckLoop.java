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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.fnl.impl.TsLoopPacket;
import org.onosproject.fnl.intf.NetworkTsCoreService;
import org.onosproject.cli.AbstractShellCommand;

import java.util.List;

/**
 * Created by mao on 1/7/16.
 */
@Command(scope = "onos", name = "CheckLoop", description = "Check if there are some loops in the network",
        detailedDescription = "Report loop-trigger packet header, DevicesIds and FlowEntries. FNL, BUPT")
public class TsCheckLoop extends AbstractShellCommand {


    @Override
    protected void execute() {
        NetworkTsCoreService networkTsCoreService = getService(NetworkTsCoreService.class);

        List<TsLoopPacket> loops = networkTsCoreService.checkLoop();

        for (TsLoopPacket loop : loops) {
            System.out.println(loop.toString());
        }
    }
}
