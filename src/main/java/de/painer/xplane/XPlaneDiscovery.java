package de.painer.xplane;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import de.painer.xplane.data.Beacon;
import de.painer.xplane.impl.DataReader;
import de.painer.xplane.impl.XPlaneInstanceUDP;

public final class XPlaneDiscovery {

    public static final int TIMEOUT_SECONDS = 30;

    public static InetSocketAddress GROUP_ADDRESS = new InetSocketAddress("239.255.1.1", 49707);

    private static final XPlaneDiscovery INSTANCE = new XPlaneDiscovery();

    public static XPlaneDiscovery getInstance() {
        return INSTANCE;
    }

    private final List<XPlaneDiscoveryListener> listeners = new ArrayList<>();

    private final Semaphore semaphore = new Semaphore(1);

    private final Map<InetSocketAddress, XPlaneInstance> instances = new HashMap<>();

    private final Map<InetSocketAddress, Long> lastBeacons = new HashMap<>();

    private volatile boolean running;

    private List<Thread> threads;

    private XPlaneDiscovery() {
    }

    public void addListener(XPlaneDiscoveryListener listener) {
        if (listeners.contains(listener)) {
            return;
        }
        listeners.add(listener);
        startThreads();
    }

    public void removeListener(XPlaneDiscoveryListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            stopThreads();
            System.out.println("threads stopped");
        }
    }

    private void processBeacon(InetSocketAddress address, Beacon beacon) {
        semaphore.acquireUninterruptibly();
        try {
            lastBeacons.put(address, System.currentTimeMillis());
            if (!instances.containsKey(address)) {
                InetSocketAddress instAddress = new InetSocketAddress(address.getAddress(), beacon.port());
                XPlaneInstance instance = new XPlaneInstanceUDP(instAddress, beacon);
                instances.put(address, instance);
                for (var listener : listeners) {
                    listener.foundInstance(instance);
                }
            }
        } finally {
            semaphore.release();
        }
    }

    private void removeLostInstances() {
        semaphore.acquireUninterruptibly();
        try {
            final long threshold = System.currentTimeMillis() - TIMEOUT_SECONDS * 1000;
            var lost = lastBeacons.entrySet().stream().filter(e -> e.getValue() < threshold).map(e -> e.getKey()).collect(toList());
            for (var l : lost) {
                lastBeacons.remove(l);
                XPlaneInstance instance = instances.remove(l);
                for (var listener : listeners) {
                    listener.lostInstance(instance);
                }
            }
        } finally {
            semaphore.release();
        }
    }

    private void startThreads() {
        if (running) {
            return;
        }

        try {
            threads = NetworkInterface.networkInterfaces().filter(ifc -> checkNetworkInterface(ifc)).map(this::createThread).filter(Objects::nonNull).collect(toList());
        } catch (SocketException ex) {
            threads = null;
        }

        if (threads == null || threads.isEmpty()) {
            running = false;
            threads = null;
        } else {
            running = true;
            threads = new ArrayList<>(threads);
            Thread gcThread = new Thread(this::lostInstancesLoop, "xplane-discover-gc");
            gcThread.setDaemon(true);
            threads.add(gcThread);
            threads.forEach(Thread::start);
        }
    }

    private void stopThreads() {
        if (!running) {
            return;
        }
        running = false;
        for (Thread t : threads) {
            try {
                System.out.println("join thread " + t.getName());
                t.interrupt();
                t.join();
                System.out.println("join finished");
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        threads = null;
    }

    private static boolean checkNetworkInterface(NetworkInterface ifc) {
        try {
            if (!ifc.isUp() || ifc.isVirtual() || ifc.isLoopback()) {
                return false;
            }
            return ifc.inetAddresses().anyMatch(a -> a instanceof Inet4Address);
        } catch (SocketException ex) {
            return false;
        }
    }

    private Thread createThread(final NetworkInterface ifc) {
        Thread thread = new Thread(() -> discoverLoop(ifc), "xplane-discover-" + ifc.getName());
        thread.setDaemon(true);
        return thread;
    }

    private void discoverLoop(final NetworkInterface ifc) {
        try {
            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(49707));
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, ifc);
            MembershipKey membership = channel.join(InetAddress.getByName("239.255.1.1"), ifc);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.order(ByteOrder.nativeOrder());
            while (running) {
                buffer.clear();
                SocketAddress address = channel.receive(buffer);
                if (!(address instanceof InetSocketAddress)) {
                    continue;
                }
                buffer.flip();
                System.out.println(buffer.remaining() + "bytes read");
                DataReader reader = new DataReader(buffer);
                if ("BECN".equals(reader.readString(5))) {
                    Beacon beacon = new Beacon(
                        reader.readUnsignedByte(),
                        reader.readUnsignedByte(),
                        reader.readInt(),
                        reader.readInt(),
                        reader.readUnsignedInt(),
                        reader.readUnsignedShort(),
                        reader.readString(500)
                    );
                    processBeacon((InetSocketAddress) address, beacon);
                }
            }
            System.out.println("dropping membership");
            membership.drop();
            System.out.println("thread finished");
        } catch (IOException ex) {
            System.err.println(ifc.getName() + ": " + ex.getMessage());
        }
    }

    private void lostInstancesLoop() {
        while (running) {
            try {
                Thread.sleep(1000);
                removeLostInstances();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

}
