package de.painer.xplane;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.DatagramChannel;

import de.painer.xplane.data.Position;

class TestMain {

    public static void main(String[] args) throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
        XPlane xplane = new XPlaneUDP(channel, new InetSocketAddress("localhost", 49000));
        xplane.addXPlaneListener(new XPlaneListener(){
            @Override
            public void receivedPosition(Position position) {
                System.out.println(position);
            }
            @Override
            public void receivedDataref(String dataref, float value) {
                System.out.println(String.format("%s: %f", dataref, value));
            }
        });
        xplane.watchPosition(1);
        xplane.watchDataref("sim/flightmodel/controls/flap_def", 1);
        xplane.sendCommand("sim/flight_controls/flaps_down");
        xplane.sendAlert("Line 1", "Line 2", "Line 3", "Line 4");

        System.in.read();

        xplane.unwatchDataref("sim/flightmodel/controls/flap_def");
        xplane.unwatchPosition();
    }

}
