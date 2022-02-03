package de.painer.xplane.impl;

import java.io.IOException;
import java.net.InetSocketAddress;

import de.painer.xplane.XPlane;
import de.painer.xplane.XPlaneInstance;
import de.painer.xplane.data.Beacon;

/**
 * Implementation of information about a found instance.
 */
public final class XPlaneInstanceUDP implements XPlaneInstance {

    /**
     * Address where the instance was found.
     */
    private final InetSocketAddress address;

    /**
     * Name of the instance.
     */
    private final String name;

    /**
     * Constructor.
     * 
     * @param address Address of the instance.
     * @param beacon  BEACON message from the instance.
     */
    public XPlaneInstanceUDP(InetSocketAddress address, Beacon beacon) {
        this.address = address;

        // construct instance name
        int major = beacon.versionNumber() / 10000;
        int minor = (beacon.versionNumber() % 10000) / 100;
        int revision = beacon.versionNumber() % 100;
        name = String.format("%s%s (X-Plane %d.%dr%d)", beacon.host(), address, major, minor, revision);
    }

    @Override
    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public XPlane connect() throws IOException {
        return new XPlaneUDP(name, address);
    }

}
