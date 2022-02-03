package de.painer.xplane.test;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.painer.xplane.XPlaneDiscovery;
import de.painer.xplane.XPlaneDiscoveryListener;
import de.painer.xplane.XPlaneInstance;

public class XPlaneDiscoveryTest {

    private static final Logger LOG = LoggerFactory.getLogger(XPlaneDiscoveryTest.class);
    
    public static void main(String[] args) throws IOException {
        XPlaneDiscoveryListener listener = new XPlaneDiscoveryListener(){

            @Override
            public void foundInstance(XPlaneInstance instance) {
                LOG.info("Found X-Plane instance {}.", instance.getName());
            }

            @Override
            public void lostInstance(XPlaneInstance instance) {
                LOG.info("Lost X-Plane instance {}.", instance.getName());
            }
            
        };

        LOG.debug("Adding listener to discovery class.");
        XPlaneDiscovery.getInstance().addListener(listener);

        LOG.info("Press any key to quit.");
        System.in.read();

        LOG.debug("Removing listener from discovery class.");
        XPlaneDiscovery.getInstance().removeListener(listener);
    }
}
