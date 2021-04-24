package de.painer.xplane;

public interface XPlane extends AutoCloseable {

    void addXPlaneListener(XPlaneListener listener);

    void removeXPlaneListener(XPlaneListener listener);

    void watchPosition(int frequency);

    void unwatchPosition();

    void sendCommand(String command);

    void watchDataref(String dataref, int frequency);

    void unwatchDataref(String dataref);

    void sendAlert(String line1, String line2, String line3, String line4);

}
