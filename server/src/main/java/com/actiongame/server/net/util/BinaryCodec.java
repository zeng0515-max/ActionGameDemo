package com.actiongame.server.net.util;

import java.nio.charset.StandardCharsets;

/**
 * 二进制消息序列化工具 (与客户端 BinarySerializer 格式一致)
 * 使用大端字节序
 */
public final class BinaryCodec {

    private BinaryCodec() {}

    /** 字符串最大字节数 (防恶意包) */
    public static final int MAX_STRING_BYTES = 4096;

    // === Read ===

    public static int readInt(byte[] buffer, int offset) {
        ensureCapacity(buffer, offset, 4);
        return (buffer[offset] << 24) | ((buffer[offset + 1] & 0xFF) << 16) |
               ((buffer[offset + 2] & 0xFF) << 8) | (buffer[offset + 3] & 0xFF);
    }

    public static long readLong(byte[] buffer, int offset) {
        ensureCapacity(buffer, offset, 8);
        return ((long) buffer[offset] << 56) | ((long) (buffer[offset + 1] & 0xFF) << 48) |
               ((long) (buffer[offset + 2] & 0xFF) << 40) | ((long) (buffer[offset + 3] & 0xFF) << 32) |
               ((long) (buffer[offset + 4] & 0xFF) << 24) | ((long) (buffer[offset + 5] & 0xFF) << 16) |
               ((long) (buffer[offset + 6] & 0xFF) << 8) | ((long) (buffer[offset + 7] & 0xFF));
    }

    public static float readFloat(byte[] buffer, int offset) {
        return Float.intBitsToFloat(readInt(buffer, offset));
    }

    public static String readString(byte[] buffer, int offset) {
        int len = readInt(buffer, offset);
        if (len <= 0) return "";
        if (len > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("String length exceeds limit: " + len + " > " + MAX_STRING_BYTES);
        }
        ensureCapacity(buffer, offset + 4, len);
        return new String(buffer, offset + 4, len, StandardCharsets.UTF_8);
    }

    // === Write ===

    public static void writeInt(byte[] buffer, int offset, int value) {
        ensureCapacity(buffer, offset, 4);
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) value;
    }

    public static void writeLong(byte[] buffer, int offset, long value) {
        ensureCapacity(buffer, offset, 8);
        buffer[offset] = (byte) (value >> 56);
        buffer[offset + 1] = (byte) (value >> 48);
        buffer[offset + 2] = (byte) (value >> 40);
        buffer[offset + 3] = (byte) (value >> 32);
        buffer[offset + 4] = (byte) (value >> 24);
        buffer[offset + 5] = (byte) (value >> 16);
        buffer[offset + 6] = (byte) (value >> 8);
        buffer[offset + 7] = (byte) value;
    }

    public static void writeFloat(byte[] buffer, int offset, float value) {
        writeInt(buffer, offset, Float.floatToIntBits(value));
    }

    public static void writeString(byte[] buffer, int offset, String value) {
        if (value == null || value.isEmpty()) {
            writeInt(buffer, offset, 0);
            return;
        }
        byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
        if (strBytes.length > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("String too long: " + strBytes.length + " > " + MAX_STRING_BYTES);
        }
        writeInt(buffer, offset, strBytes.length);
        ensureCapacity(buffer, offset + 4, strBytes.length);
        System.arraycopy(strBytes, 0, buffer, offset + 4, strBytes.length);
    }

    // === Size ===

    public static int stringSize(String value) {
        if (value == null || value.isEmpty()) return 4;
        return 4 + value.getBytes(StandardCharsets.UTF_8).length;
    }

    // === Internal ===

    private static void ensureCapacity(byte[] buffer, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > buffer.length) {
            throw new IllegalArgumentException(
                String.format("Buffer overflow: offset=%d, length=%d, bufferSize=%d",
                    offset, length, buffer.length));
        }
    }
}
