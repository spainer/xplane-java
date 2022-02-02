package de.painer.xplane.impl;

import java.io.IOException;
import java.net.InetSocketAddress;

import de.painer.xplane.XPlane;
import de.painer.xplane.XPlaneInstance;
import de.painer.xplane.data.Beacon;

public final class XPlaneInstanceUDP implements XPlaneInstance {

    private final InetSocketAddress address;

    private final String name;

    public XPlaneInstanceUDP(InetSocketAddress address, Beacon beacon) {
        this.address = address;

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
