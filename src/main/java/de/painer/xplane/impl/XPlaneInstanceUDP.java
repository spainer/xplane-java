package de.painer.xplane.impl;

import java.io.IOException;
import java.net.InetSocketAddress;

import de.painer.xplane.XPlane;
import de.painer.xplane.XPlaneInstance;
import de.painer.xplane.data.Beacon;

public final class XPlaneInstanceUDP implements XPlaneInstance {

    private final InetSocketAddress address;

    public XPlaneInstanceUDP(InetSocketAddress address, Beacon beacon) {
        this.address = address;
    }

    @Override
    public InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public XPlane connect() throws IOException {
        return new XPlaneUDP(address);
    }
    
}
