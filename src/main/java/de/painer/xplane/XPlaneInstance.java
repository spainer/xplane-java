package de.painer.xplane;

import java.io.IOException;
import java.net.SocketAddress;

public interface XPlaneInstance {

    SocketAddress getAddress();

    String getName();

    XPlane connect() throws IOException;

}
