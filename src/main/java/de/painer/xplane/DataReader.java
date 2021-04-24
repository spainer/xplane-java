package de.painer.xplane;

import java.nio.ByteBuffer;

final class DataReader {

    private final ByteBuffer buffer;

    DataReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public byte readByte() {
        return buffer.get();
    }

    public int readUnsignedByte() {
        return Byte.toUnsignedInt(readByte());
    }

    public short readShort() {
        return buffer.getShort();
    }

    public int readUnsignedShort() {
        return Short.toUnsignedInt(readShort());
    }

    public int readInt() {
        return buffer.getInt();
    }

    public long readUnsignedInt() {
        return Integer.toUnsignedLong(readInt());
    }

    public long readLong() {
        return buffer.getLong();
    }

    public float readFloat() {
        return buffer.getFloat();
    }

    public double readDouble() {
        return buffer.getDouble();
    }

    public String readString(int length) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (byte b = buffer.get(); b != 0 && index < length - 1; b = buffer.get(), index++) {
            sb.append((char) b);
        }
        for (index++; index < length; index++) {
            buffer.get();
        }
        return sb.toString();
    }
}
