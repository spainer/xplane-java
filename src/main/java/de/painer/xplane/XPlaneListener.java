package de.painer.xplane;

import de.painer.xplane.data.Position;

/**
 * Listener to data received from X-Plane.
 */
public interface XPlaneListener {

    /**
     * Recevied current position.
     * 
     * @param position Currently received position.
     */
    void receivedPosition(Position position);

    /**
     * Received value for a registered dataref.
     * 
     * @param dataref Id of the dataref.
     * @param value Current value of the dataref.
     */
    void receivedDataref(String dataref, float value);

}
