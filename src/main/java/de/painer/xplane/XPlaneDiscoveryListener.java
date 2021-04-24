package de.painer.xplane;

public interface XPlaneDiscoveryListener {

    void foundInstance(XPlaneInstance instance);

    void lostInstance(XPlaneInstance instance);

}
