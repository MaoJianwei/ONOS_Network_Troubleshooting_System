package org.onosproject.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.net.NetworkTroubleshoot.NetworkTS;

/**
 * Created by mao on 12/3/15.
 */


@Command(scope = "onos", name="FNL",
        description = "Network TroubleShoot debug interface")
public class NetworkTScommand extends AbstractShellCommand {


    protected NetworkTS networkTS;

    @Override
    protected void execute(){

        print("Up!");
        networkTS = get(NetworkTS.class);
        networkTS.debug();
        print("Down.");
    }

}
