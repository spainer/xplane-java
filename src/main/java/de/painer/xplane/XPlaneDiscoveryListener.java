package de.painer.xplane;

/**
 * Listener for found and lost X-Plane instances.
 * 
 * @see XPlaneDiscovery
 */
public interface XPlaneDiscoveryListener {

    /**
     * A new X-Plane instance has been found.
     * 
     * @param instance Found X-Plane instance.
     */
    void foundInstance(XPlaneInstance instance);

    /**
     * A X-Plane instance has been lost.
     * 
     * @param instance Lost X-Plane instance.
     */
    void lostInstance(XPlaneInstance instance);

}
