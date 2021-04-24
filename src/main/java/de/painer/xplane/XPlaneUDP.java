package de.painer.xplane;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;

import de.painer.xplane.data.Position;

final class XPlaneUDP implements XPlane {

    private final Thread receiveThread;
    
    private final DatagramChannel channel;

    private final SocketAddress address;

    private final List<XPlaneListener> listeners = new ArrayList<>();

    private final List<String> watchedDatarefs = new ArrayList<>();

    XPlaneUDP(DatagramChannel channel, SocketAddress address) {
        this.channel = channel;
        this.address = address;

        receiveThread = new Thread(this::receiveLoop, "xplane-receive");
        receiveThread.setDaemon(true);
        receiveThread.start();
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
        DataWriter writer = new DataWriter(8);
        writer.writeString("RPOS");
        writer.writeString(Integer.toString(limitFrequency(frequency)));
        send(writer.export());
    }

    @Override
    public void unwatchPosition() {
        watchPosition(0);
    }

    @Override
    public void sendCommand(String command) {
        DataWriter writer = new DataWriter(500);
        writer.writeString("CMND");
        writer.writeString(command);
        send(writer.export());
    }

    @Override
    public void watchDataref(String dataref, int frequency) {
        int index = watchedDatarefs.indexOf(dataref);
        if (index < 0) {
            index = watchedDatarefs.size();
            watchedDatarefs.add(dataref);
        }
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
        DataWriter writer = new DataWriter(965);
        writer.writeString("ALRT");
        writer.writeString(line1 != null ? line1 : "", 240);
        writer.writeString(line2 != null ? line2 : "", 240);
        writer.writeString(line3 != null ? line3 : "", 240);
        writer.writeString(line4 != null ? line4 : "", 240);
        send(writer.export());
    }

    private void send(ByteBuffer buffer) {
        try {
            channel.send(buffer, address);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void receivedRpos(DataReader reader) {
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
            reader.readFloat()
        );
        for (var listener : listeners) {
            listener.receivedPosition(pos);
        }
    }

    private void receivedRref(DataReader reader) {
        int index = reader.readInt();
        float value = reader.readFloat();
        String dataref = watchedDatarefs.get(index);
        for (var listener : listeners) {
            listener.receivedDataref(dataref, value);
        }
    }

    private void receiveLoop() {
        ByteBuffer buffer = ByteBuffer.allocate(1500);
        buffer.order(ByteOrder.nativeOrder());
        while (true) {
            try {
                buffer.clear();
                channel.receive(buffer);
                buffer.flip();
                DataReader reader = new DataReader(buffer);
                String msgType = reader.readString(5);
                switch (msgType) {
                    case "RPOS" -> receivedRpos(reader);
                    case "RREF" -> receivedRref(reader);
                    default -> System.out.println("Unknown message type received: " + msgType);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static int limitFrequency(int frequency) {
        return Math.min(Math.max(frequency, 0), 99);
    }

}
