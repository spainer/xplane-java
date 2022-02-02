package de.painer.xplane;

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
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.painer.xplane.data.Beacon;
import de.painer.xplane.impl.DataReader;
import de.painer.xplane.impl.XPlaneInstanceUDP;

/**
 * Singletion class for discovery of X-Plane instances.
 * 
 * <p>
 * Clients should register as a listener for X-Plane instances. The client will
 * then be informed about new and missing instances until it is deregistered. If
 * there are already instances known, the method <code>foundInstance</code> will
 * be called immediately for each instace.
 * </p>
 * 
 * <p>
 * The discovery of new instances is done in background threads. There will be
 * one thread per network interface and one thread for removing X-Plane
 * instances when there was no beacon for more than 30 seconds. The threads are
 * started automatically when the first listener registered and are stopped and
 * deleted automatically when the last listener is removed.
 * </p>
 */
public final class XPlaneDiscovery {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(XPlaneDiscovery.class);

    /**
     * Timeout for removing X-Plane instances when there is no beacon any more.
     */
    public static final int TIMEOUT_SECONDS = 30;

    /**
     * UDP Group address where X-Plane sends the beacons.
     */
    public static InetSocketAddress GROUP_ADDRESS = new InetSocketAddress("239.255.1.1", 49707);

    /**
     * Singleton instance of the class.
     */
    private static final XPlaneDiscovery INSTANCE = new XPlaneDiscovery();

    /**
     * Returns the singleton instance of this class.
     * 
     * @return Singleton instance.
     */
    public static XPlaneDiscovery getInstance() {
        return INSTANCE;
    }

    /**
     * List of listeners actually registered.
     */
    private final List<XPlaneDiscoveryListener> listeners = new ArrayList<>();

    /**
     * Semaphore to synchronize access to the instances across threads.
     */
    private final Semaphore semaphore = new Semaphore(1);

    /**
     * Currently known X-Plane instances.
     */
    private final Map<InetSocketAddress, XPlaneInstance> instances = new HashMap<>();

    /**
     * Last time a beacon from an instance was received.
     */
    private final Map<InetSocketAddress, Long> lastBeacons = new HashMap<>();

    /**
     * Are the discovery threads currently running?
     */
    private volatile boolean running;

    /**
     * List with discovery threads.
     */
    private List<Thread> threads;

    /**
     * Constructor.
     */
    private XPlaneDiscovery() {
    }

    /**
     * Adds a listener to the discovery class.
     * 
     * <p>
     * When the discovery threads are currently running, <code>foundInstace</code>
     * is directly called for each known instance. Otherwise, the discovery threads
     * are started.
     * </p>
     * 
     * @param listener Listener to register.
     */
    public void addListener(XPlaneDiscoveryListener listener) {
        // do nothing if the listener is already registered
        if (listeners.contains(listener)) {
            return;
        }

        // add listener to the list
        listeners.add(listener);

        // send known instances or start threads
        if (running) {
            semaphore.acquireUninterruptibly();
            try {
                for (XPlaneInstance instance : instances.values()) {
                    listener.foundInstance(instance);
                }
            } finally {
                semaphore.release();
            }
        } else {
            startThreads();
        }
    }

    /**
     * Removes a listener from the discovery class.
     * 
     * <p>
     * If this is the last listener, all discovery threads are stopped.
     * </p>
     * 
     * @param listener Listener to remove.
     */
    public void removeListener(XPlaneDiscoveryListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            stopThreads();
        }
    }

    /**
     * Start discovery threads for all valid network interfaces.
     */
    private void startThreads() {
        // do nothin if threads are already running
        if (running) {
            return;
        }

        // create list with threads for all valid network interfaces
        LOG.debug("Starting threads for X-Plane discovery.");
        try {
            threads = NetworkInterface.networkInterfaces().filter(ifc -> checkNetworkInterface(ifc))
                    .map(this::createThread).filter(Objects::nonNull).toList();
        } catch (SocketException ex) {
            threads = null;
        }

        // stop if no threads could be created
        if (threads == null || threads.isEmpty()) {
            running = false;
            threads = null;
            return;
        }

        // add thread for removing lost instances and start all threads
        running = true;
        threads = new ArrayList<>(threads);
        Thread gcThread = new Thread(this::lostInstancesLoop, "xplane-discover-gc");
        gcThread.setDaemon(true);
        threads.add(gcThread);
        threads.forEach(Thread::start);
    }

    /**
     * Creates a discovery thread for a given network interface.
     * 
     * @param ifc Network interface to create the thread for.
     * @return Discovery thread for listening on the network interface.
     */
    private Thread createThread(final NetworkInterface ifc) {
        LOG.debug("Create discovery thread for interface {}.", ifc.getName());
        Thread thread = new Thread(() -> discoverLoop(ifc), "xplane-discover-" + ifc.getName());
        thread.setDaemon(true);
        return thread;
    }

    /**
     * Stops all discovery threads.
     */
    private void stopThreads() {
        // do nothing if not currently running
        if (!running) {
            return;
        }
        LOG.debug("Stopping threads for X-Plane discovery.");

        // stop all threads and clear instances
        running = false;
        for (Thread t : threads) {
            try {
                t.interrupt();
                t.join();
            } catch (InterruptedException ex) {
                // this is normal behaviour
            }
        }
        threads = null;
        instances.clear();
        lastBeacons.clear();
    }

    /**
     * Execution loop of discovery threads.
     * 
     * @param ifc Network interface to check for instances.
     */
    private void discoverLoop(final NetworkInterface ifc) {
        // membership of UDP broadcast group
        MembershipKey membership = null;

        try {
            // create UDP channel to listen for BEACON broadcasts
            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(49707));
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, ifc);
            membership = channel.join(InetAddress.getByName("239.255.1.1"), ifc);

            // create buffer for reveiving data
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.order(ByteOrder.nativeOrder());

            // execution loop
            while (running) {
                // receive next message
                buffer.clear();
                SocketAddress address = channel.receive(buffer);
                if (!(address instanceof InetSocketAddress)) {
                    continue;
                }
                buffer.flip();
                LOG.debug("Received {} bytes from {}.", buffer.remaining(), address);

                // parse data and process beacon if BECN message
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
        } catch (ClosedByInterruptException ex) {
            // this is normal behaviour during stopping of threads
        } catch (IOException ex) {
            LOG.error("Error during discovery on interface {}.", ifc.getName(), ex);
        }

        // drop membership of UDP group if successfully joined
        if (membership != null) {
            membership.drop();
        }
    }

    /**
     * Process a receviced beacon.
     * 
     * @param address Address from which the beacon was sent.
     * @param beacon  Content of the beacon message.
     */
    private void processBeacon(InetSocketAddress address, Beacon beacon) {
        semaphore.acquireUninterruptibly();
        try {
            // register time of currently recevied beacon
            lastBeacons.put(address, System.currentTimeMillis());

            // register instance and inform listeners if this is a newly found instance
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

    /**
     * Execution loop of thread for removing lost instances.
     */
    private void lostInstancesLoop() {
        while (running) {
            try {
                Thread.sleep(1000);
                removeLostInstances();
            } catch (InterruptedException ex) {
                // this is normal behaviour during stopping of threads
            }
        }
    }

    /**
     * Remove all lost instances.
     */
    private void removeLostInstances() {
        semaphore.acquireUninterruptibly();
        try {
            // find all instances where the last beacon was too long ago
            final long threshold = System.currentTimeMillis() - TIMEOUT_SECONDS * 1000;
            List<InetSocketAddress> lost = lastBeacons.entrySet().stream().filter(e -> e.getValue() < threshold)
                    .map(e -> e.getKey()).toList();

            // remove lost instances from the list and inform listeners
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

    /**
     * Checks whether a network interface is valid for discovery.
     * 
     * @param ifc Network interface to check.
     * @return Is the network interface valid for discovery?
     */
    private static boolean checkNetworkInterface(NetworkInterface ifc) {
        try {
            // basic checks
            if (!ifc.isUp() || ifc.isVirtual() || ifc.isLoopback()) {
                return false;
            }

            // the network interface should have an IPv4 address
            return ifc.inetAddresses().anyMatch(Inet4Address.class::isInstance);
        } catch (SocketException ex) {
            return false;
        }
    }

}
