using System;
using System.Text;

namespace ActionGameDemo.Protocol
{
    /// <summary>
    /// 简易二进制序列化工具 (替代Protobuf-csharp, 避免外部依赖)
    /// 使用大端字节序
    /// </summary>
    public static class BinarySerializer
    {

        // === Write ===

        public static void WriteInt(byte[] buffer, int offset, int value)
        {
            buffer[offset] = (byte)(value >> 24);
            buffer[offset + 1] = (byte)(value >> 16);
            buffer[offset + 2] = (byte)(value >> 8);
            buffer[offset + 3] = (byte)value;
        }

        public static void WriteLong(byte[] buffer, int offset, long value)
        {
            buffer[offset] = (byte)(value >> 56);
            buffer[offset + 1] = (byte)(value >> 48);
            buffer[offset + 2] = (byte)(value >> 40);
            buffer[offset + 3] = (byte)(value >> 32);
            buffer[offset + 4] = (byte)(value >> 24);
            buffer[offset + 5] = (byte)(value >> 16);
            buffer[offset + 6] = (byte)(value >> 8);
            buffer[offset + 7] = (byte)value;
        }

        public static void WriteFloat(byte[] buffer, int offset, float value)
        {
            int intValue = BitConverter.ToInt32(BitConverter.GetBytes(value), 0);
            WriteInt(buffer, offset, intValue);
        }

        public static void WriteBool(byte[] buffer, int offset, bool value)
        {
            buffer[offset] = value ? (byte)1 : (byte)0;
        }

        public static void WriteString(byte[] buffer, int offset, string value, int maxBytes)
        {
            if (string.IsNullOrEmpty(value))
            {
                WriteInt(buffer, offset, 0);
                return;
            }
            byte[] strBytes = Encoding.UTF8.GetBytes(value);
            int len = Math.Min(strBytes.Length, maxBytes);
            WriteInt(buffer, offset, len);
            Array.Copy(strBytes, 0, buffer, offset + 4, len);
        }

        // === Read ===

        public static int ReadInt(byte[] buffer, int offset)
        {
            return (buffer[offset] << 24) | (buffer[offset + 1] << 16) | (buffer[offset + 2] << 8) | buffer[offset + 3];
        }

        public static long ReadLong(byte[] buffer, int offset)
        {
            return ((long)buffer[offset] << 56) | ((long)buffer[offset + 1] << 48) |
                   ((long)buffer[offset + 2] << 40) | ((long)buffer[offset + 3] << 32) |
                   ((long)buffer[offset + 4] << 24) | ((long)buffer[offset + 5] << 16) |
                   ((long)buffer[offset + 6] << 8) | buffer[offset + 7];
        }

        public static float ReadFloat(byte[] buffer, int offset)
        {
            int intValue = ReadInt(buffer, offset);
            return BitConverter.ToSingle(BitConverter.GetBytes(intValue), 0);
        }

        public static bool ReadBool(byte[] buffer, int offset)
        {
            return buffer[offset] != 0;
        }

        public static string ReadString(byte[] buffer, int offset)
        {
            int len = ReadInt(buffer, offset);
            if (len <= 0) return string.Empty;
            return Encoding.UTF8.GetString(buffer, offset + 4, len);
        }

        // === Size helpers ===

        public static int StringSize(string value)
        {
            if (string.IsNullOrEmpty(value)) return 4;
            return 4 + Encoding.UTF8.GetByteCount(value);
        }
    }
}
