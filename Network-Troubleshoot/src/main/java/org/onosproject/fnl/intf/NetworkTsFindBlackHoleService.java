package org.onosproject.fnl.intf;

import org.onosproject.fnl.impl.TsFindBhPacket;
import org.onosproject.net.flow.TrafficSelector;

import java.util.List;

/**
 * Created by mao on 12/22/15.
 */
public interface NetworkTsFindBlackHoleService {
    /**
     * Enter of Blackhole tracking.
     * @param trafficSelector   the 12 tuples message
     * @return the result that contains the black hole location
     */
    List<TsFindBhPacket> findBlackHole(TrafficSelector trafficSelector);
}


