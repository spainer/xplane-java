package de.painer.xplane.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.painer.xplane.XPlane;
import de.painer.xplane.XPlaneListener;
import de.painer.xplane.data.Position;

/**
 * Implementation of X-Plane connection.
 */
public final class XPlaneUDP implements XPlane {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(XPlaneUDP.class);

    /**
     * Name of the instance.
     */
    private final String name;

    /**
     * Thread for receiving data from X-Plane.
     */
    private final Thread receiveThread;

    /**
     * UDP channel for communication with X-Plane.
     */
    private final DatagramChannel channel;

    /**
     * Address of the instance.
     */
    private final InetSocketAddress address;

    /**
     * Listeners for data from X-Plane.
     */
    private final List<XPlaneListener> listeners = new ArrayList<>();

    /**
     * Currently watched datarefs.
     */
    private final List<String> watchedDatarefs = new ArrayList<>();

    /**
     * Constructor.
     * 
     * @param name    Name of the instance.
     * @param address Address of the instance.
     * @throws IOException In case of connection error.
     */
    public XPlaneUDP(String name, InetSocketAddress address) throws IOException {
        this.name = name;
        this.channel = DatagramChannel.open(StandardProtocolFamily.INET);
        this.address = address;

        // create and start thread for receiving data
        receiveThread = new Thread(this::receiveLoop, "xplane-receive");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addXPlaneListener(XPlaneListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeXPlaneListener(XPlaneListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void watchPosition(int frequency) {
        frequency = limitFrequency(frequency);
        LOG.debug("Watch position with frequency {} Hz.", frequency);

        DataWriter writer = new DataWriter(8);
        writer.writeString("RPOS");
        writer.writeString(Integer.toString(frequency));
        send(writer.export());
    }

    @Override
    public void unwatchPosition() {
        watchPosition(0);
    }

    @Override
    public void sendCommand(String command) {
        LOG.debug("Sending command {}.", command);

        DataWriter writer = new DataWriter(500);
        writer.writeString("CMND");
        writer.writeString(command);
        send(writer.export());
    }

    @Override
    public void watchDataref(String dataref, int frequency) {
        // index in the list is used as ID for messages
        int index = watchedDatarefs.indexOf(dataref);
        if (index < 0) {
            index = watchedDatarefs.size();
            watchedDatarefs.add(dataref);
        }
        LOG.debug("Watching dataref {} with ID {} and frequency {}.", dataref, index, frequency);

        DataWriter writer = new DataWriter(413);
        writer.writeString("RREF");
        writer.writeInt(frequency);
        writer.writeInt(index);
        writer.writeString(dataref, 400);
        send(writer.export());
    }

    @Override
    public void unwatchDataref(String dataref) {
        watchDataref(dataref, 0);
    }

    @Override
    public void sendAlert(String line1, String line2, String line3, String line4) {
        LOG.debug("Sending alert {}; {}; {}; {}.", line1, line2, line3, line4);

        DataWriter writer = new DataWriter(965);
        writer.writeString("ALRT");
        writer.writeString(line1 != null ? line1 : "", 240);
        writer.writeString(line2 != null ? line2 : "", 240);
        writer.writeString(line3 != null ? line3 : "", 240);
        writer.writeString(line4 != null ? line4 : "", 240);
        send(writer.export());
    }

    @Override
    public void close() throws Exception {
        unwatchPosition();
        for (String dataref : watchedDatarefs) {
            unwatchDataref(dataref);
        }
    }

    /**
     * Sends a message to X-Plane.
     * 
     * @param buffer Content of the message to send.
     */
    private void send(ByteBuffer buffer) {
        try {
            channel.send(buffer, address);
        } catch (IOException ex) {
            LOG.error("Error during sending data to X-Plane.", ex);
        }
    }

    /**
     * Process position received from X-Plane.
     * 
     * @param reader Reader for reading content.
     */
    private void receivedRpos(DataReader reader) {
        // parse data
        Position pos = new Position(
                reader.readDouble(),
                reader.readDouble(),
                reader.readDouble(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat(),
                reader.readFloat());
        LOG.debug("Received position {}.", pos);

        // send data to listeners
        for (var listener : listeners) {
            listener.receivedPosition(pos);
        }
    }

    /**
     * Processed received dataref.
     * 
     * @param reader Reader to read message data.
     */
    private void receivedRref(DataReader reader) {
        // read message data
        int index = reader.readInt();
        float value = reader.readFloat();
        String dataref = watchedDatarefs.get(index);
        LOG.debug("Received dataref {} with ID {} and value {}.", dataref, index, value);

        // inform listeners
        for (var listener : listeners) {
            listener.receivedDataref(dataref, value);
        }
    }

    /**
     * Execution loop of receiver thread.
     */
    private void receiveLoop() {
        // allocate buffer to receive messages
        ByteBuffer buffer = ByteBuffer.allocate(1500);
        buffer.order(ByteOrder.nativeOrder());

        while (true) {
            try {
                // receive data
                buffer.clear();
                channel.receive(buffer);

                // create reader for data
                buffer.flip();
                DataReader reader = new DataReader(buffer);

                // handle the message according to it's type
                String msgType = reader.readString(5);
                switch (msgType) {
                    case "RPOS" -> receivedRpos(reader);
                    case "RREF" -> receivedRref(reader);
                    default -> LOG.warn("Unknown message type received: {}.", msgType);
                }
            } catch (IOException ex) {
                LOG.error("Error during receiving messge.", ex);
            }
        }
    }

    /**
     * Limits a frequency to the interval [0, 99];
     */
    private static int limitFrequency(int frequency) {
        return Math.min(Math.max(frequency, 0), 99);
    }

}
