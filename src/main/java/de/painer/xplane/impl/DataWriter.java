package de.painer.xplane.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class DataWriter {

    private final ByteBuffer buffer;

    DataWriter(int size) {
        buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.nativeOrder());
    }

    public ByteBuffer export() {
        return buffer.flip();
    }

    public void writeByte(byte data) {
        buffer.put(data);
    }

    public void writeShort(short data) {
        buffer.putShort(data);
    }

    public void writeInt(int data) {
        buffer.putInt(data);
    }

    public void writeLong(long data) {
        buffer.putLong(data);
    }

    public void writeFloat(float data) {
        buffer.putFloat(data);
    }

    public void writeDouble(double data) {
        buffer.putDouble(data);
    }

    public void writeString(String data) {
        for (int index = 0; index < data.length(); index++) {
            buffer.put((byte) data.charAt(index));
        }
        buffer.put((byte) 0);
    }

    public void writeString(String data, int length) {
        int index;
        for (index = 0; index < data.length() && index < length - 1; index++) {
            buffer.put((byte) data.charAt(index));
        }
        buffer.put((byte) 0);
        for(index++; index < length; index++) {
            buffer.put((byte) 32);
        }
    }

}
