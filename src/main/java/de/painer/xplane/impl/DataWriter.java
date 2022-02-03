package de.painer.xplane.impl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Conversion utility for sending data.
 */
public final class DataWriter {

    /**
     * Buffer that will be filled with the data.
     */
    private final ByteBuffer buffer;

    /**
     * Constructor.
     * 
     * @param size Size of the buffer for constructing data.
     */
    public DataWriter(int size) {
        buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.nativeOrder());
    }

    /**
     * Finished the data in the buffer and returns it.
     * 
     * @return Buffer with created byte data.
     */
    public ByteBuffer export() {
        return buffer.flip();
    }

    /**
     * Writes a byte.
     * 
     * @param data Byte to write to the buffer.
     */
    public void writeByte(byte data) {
        buffer.put(data);
    }

    /**
     * Writes a short.
     * 
     * @param data Short to write to the buffer.
     */
    public void writeShort(short data) {
        buffer.putShort(data);
    }

    /**
     * Writes an int.
     * 
     * @param data Int to write to the buffer.
     */
    public void writeInt(int data) {
        buffer.putInt(data);
    }

    /**
     * Writes a long.
     * 
     * @param data Long to write to the buffer.
     */
    public void writeLong(long data) {
        buffer.putLong(data);
    }

    /**
     * Writes a float.
     * 
     * @param data Float to write to the buffer.
     */
    public void writeFloat(float data) {
        buffer.putFloat(data);
    }

    /**
     * Writes a double.
     * 
     * @param data Double to write to the buffer.
     */
    public void writeDouble(double data) {
        buffer.putDouble(data);
    }

    /**
     * Writes a string.
     * 
     * @param data String to write to the buffer.
     */
    public void writeString(String data) {
        for (int index = 0; index < data.length(); index++) {
            buffer.put((byte) data.charAt(index));
        }
        buffer.put((byte) 0);
    }

    /**
     * Writes a string with fixed length.
     * 
     * <p>
     * Spaces are added to the buffer after the string until the fixed length is
     * reached.
     * </p>
     * 
     * @param data   String to write to the buffer.
     * @param length Length of data to write to the buffer.
     */
    public void writeString(String data, int length) {
        int index;
        for (index = 0; index < data.length() && index < length - 1; index++) {
            buffer.put((byte) data.charAt(index));
        }
        buffer.put((byte) 0);
        for (index++; index < length; index++) {
            buffer.put((byte) 32);
        }
    }

}
