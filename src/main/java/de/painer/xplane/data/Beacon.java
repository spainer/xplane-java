package de.painer.xplane.data;

/**
 * BEACON message.
 */
public record Beacon(
    int beaconMajorVersion,
    int beaconMinorVersion,
    int applicationHostId,
    int versionNumber,
    long role,
    int port,
    String host
) { }
