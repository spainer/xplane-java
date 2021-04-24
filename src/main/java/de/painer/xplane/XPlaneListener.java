package de.painer.xplane;

import de.painer.xplane.data.Position;

public interface XPlaneListener {

    void receivedPosition(Position position);

    void receivedDataref(String dataref, float value);

}
