package de.painer.xplane;

/**
 * Connected X-Plane instance.
 */
public interface XPlane extends AutoCloseable {

    /**
     * Returns the name of the X-Plane instance.
     */
    String getName();

    /**
     * Adds a listener for data received from X-Plane.
     * 
     * @param listener Listener to add.
     */
    void addXPlaneListener(XPlaneListener listener);

    /**
     * Removes a listener for data received from X-Plane.
     * 
     * @param listener Listener to remove.
     */
    void removeXPlaneListener(XPlaneListener listener);

    /**
     * Instruct X-Plane to send the position with the given frequency.
     * 
     * @param frequency Number of positions to send per second.
     */
    void watchPosition(int frequency);

    /**
     * Instruct X-Plane to not send any further positions.
     */
    void unwatchPosition();

    /**
     * Send a command to X-Plane.
     * 
     * @param command Command to send.
     */
    void sendCommand(String command);

    /**
     * Instruct X-Plane to send a dataref with the given frequency.
     * 
     * @param dataref   Dataref to send.
     * @param frequency Number of values per second (0 for just once).
     */
    void watchDataref(String dataref, int frequency);

    /**
     * Instruct X-Plane to not send a dataref any more.
     * 
     * @param dataref Dataref to not send any more.
     */
    void unwatchDataref(String dataref);

    /**
     * Show an alert message in X-Plane.
     * 
     * @param line1 First line of the message.
     * @param line2 Second line of the message.
     * @param line3 Third line of the message.
     * @param line4 Fourth line of the message.
     */
    void sendAlert(String line1, String line2, String line3, String line4);

}
