package org.onosproject.fnl.intf;

import org.onosproject.fnl.impl.TsLoopPacket;

import java.util.List;

/**
 * Created by mao on 1/7/16.
 */
public interface NetWorkTsCheckLoopService {

    /**
     * Enter of Loop Detecting.
     * @return
     */
    List<TsLoopPacket> checkLoop();
}
