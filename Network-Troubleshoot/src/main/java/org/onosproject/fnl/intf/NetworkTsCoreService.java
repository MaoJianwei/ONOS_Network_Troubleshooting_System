
package org.onosproject.fnl.intf;

import org.onosproject.fnl.impl.TsFindBhPacket;
import org.onosproject.fnl.impl.TsLoopPacket;
import org.onosproject.net.flow.TrafficSelector;

import java.util.List;

/**
 *
 */
public interface NetworkTsCoreService {

    /**
     * for debug convenience.
     */
    void debug();

    /**
     * interface with NorthBound.
     * @return
     */
    List<TsLoopPacket> checkLoop();

    /**
     * interface with NorthBound.
     * @param trafficSelector   the 12 tuples message
     * @return the result that contains the black hole location
     */
    List<TsFindBhPacket> findBlackHole(TrafficSelector trafficSelector);
}
