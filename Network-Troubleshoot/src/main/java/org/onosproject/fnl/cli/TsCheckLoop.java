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
