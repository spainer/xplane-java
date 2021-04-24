package de.painer.xplane.data;

public record Position(
    double longitude,
    double latitude,
    double elevationMSL,
    float elevationAGL,
    float pitch,
    float heading,
    float roll,
    float speedX,
    float speedY,
    float speedZ,
    float rollRate,
    float pitchRate,
    float yawRate
) { }