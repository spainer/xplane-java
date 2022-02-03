package de.painer.xplane.impl;

import java.nio.ByteBuffer;

/**
 * Conversion utility from incoming messages to data.
 * 
 * <p>
 * When reading unsigned data, it is converted to the next bigger data type to
 * avoid possible overflows as Java does not know unsigned data types.
 * </p>
 */
public final class DataReader {

    /**
     * Buffer to read from.
     */
    private final ByteBuffer buffer;

    /**
     * Constructor.
     * 
     * <p>
     * The buffer with the message data should already be set to the right byte
     * order and repeated such that reading starts at the beginning.
     * </p>
     * 
     * @param buffer Message data to read from.
     */
    public DataReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Reads one byte.
     * 
     * @return Byte read from the buffer.
     */
    public byte readByte() {
        return buffer.get();
    }

    /**
     * Read one unsigned byte.
     * 
     * @return Unsigned byte read from the buffer.
     */
    public int readUnsignedByte() {
        return Byte.toUnsignedInt(readByte());
    }

    /**
     * Reads one short.
     * 
     * @return Short read from the buffer.
     */
    public short readShort() {
        return buffer.getShort();
    }

    /**
     * Reads on unsigned short.
     * 
     * @return Unsigned short read from the buffer.
     */
    public int readUnsignedShort() {
        return Short.toUnsignedInt(readShort());
    }

    /**
     * Reads one int.
     * 
     * @return Int read from the buffer.
     */
    public int readInt() {
        return buffer.getInt();
    }

    /**
     * Reads one unsigned int.
     * 
     * @return Unsigned int read from the buffer.
     */
    public long readUnsignedInt() {
        return Integer.toUnsignedLong(readInt());
    }

    /**
     * Reads one long.
     * 
     * @return Long read from the buffer.
     */
    public long readLong() {
        return buffer.getLong();
    }

    /**
     * Reads one float from the buffer.
     * 
     * @return Float read from the buffer.
     */
    public float readFloat() {
        return buffer.getFloat();
    }

    /**
     * Reads one double from the buffer.
     * 
     * @return Double read from the buffer.
     */
    public double readDouble() {
        return buffer.getDouble();
    }

    /**
     * Reads a string with maximum length from the buffer.
     * 
     * <p>
     * Reads characters from the buffer until the maximum length is reached or the
     * null-termination is received.
     * </p>
     * 
     * @param maxLength Maximum length of the string.
     * @return String read from the buffer.
     */
    public String readString(int maxLength) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        for (byte b = buffer.get(); b != 0 && index < maxLength; b = buffer.get(), index++) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * Reads a string with fixed byte length from the buffer.
     * 
     * <p>
     * Reads the given number of bytes from the buffer, but only constructs a string
     * up to the null-termination.
     * </p>
     * 
     * @param length Number of bytes to read.
     * @return String read from the buffer.
     */
    public String readFullString(int length) {
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
