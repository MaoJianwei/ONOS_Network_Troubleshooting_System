package org.onosproject.fnl.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.fnl.intf.NetworkTsCoreService;

/**
 * Created by mao on 12/3/15.
 */


@Command(scope = "onos", name = "FNL",
        description = "Network TroubleShoot debug interface")
public class TsDebugCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        print("Up!");
        NetworkTsCoreService networkTS = get(NetworkTsCoreService.class);
        networkTS.debug();
        print("Down.");
    }
}
