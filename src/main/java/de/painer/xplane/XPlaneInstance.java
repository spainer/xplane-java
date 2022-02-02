package de.painer.xplane;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * X-Plane instance reachable over the network.
 * 
 * <p>
 * X-Plane instance can be found with the class {@link XPlaneDiscovery}.
 * </p>
 */
public interface XPlaneInstance {

    /**
     * Returns the address to the X-Plane instance.
     */
    SocketAddress getAddress();

    /**
     * Returns a name of the X-Plane instance.
     */
    String getName();

    /**
     * Connnects to the X-Plane instance.
     */
    XPlane connect() throws IOException;

}
