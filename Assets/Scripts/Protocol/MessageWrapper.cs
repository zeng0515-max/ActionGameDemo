using System;

namespace ActionGameDemo.Protocol
{
    /// <summary>
    /// 消息包装器 (每条WebSocket消息的外层信封)
    /// 帧格式: [消息ID(4)] [协议版本(4)] [序列号(8)] [Payload长度(4)] [Payload]
    /// (无外层长度前缀, WebSocket帧自带长度)
    /// </summary>
    public class MessageWrapper
    {
        public MessageId MessageId;
        public int ProtocolVersion;
        public long SequenceId;
        public byte[] Payload;

        public int HeaderSize => 4 + 4 + 8 + 4; // id + version + seq + payloadLen

        public byte[] ToByteArray()
        {
            int payloadLen = Payload?.Length ?? 0;
            byte[] buffer = new byte[HeaderSize + payloadLen];

            int offset = 0;
            BinarySerializer.WriteInt(buffer, offset, (int)MessageId); offset += 4;
            BinarySerializer.WriteInt(buffer, offset, ProtocolVersion); offset += 4;
            BinarySerializer.WriteLong(buffer, offset, SequenceId); offset += 8;
            BinarySerializer.WriteInt(buffer, offset, payloadLen); offset += 4;

            if (payloadLen > 0)
            {
                Array.Copy(Payload, 0, buffer, offset, payloadLen);
            }

            return buffer;
        }

        public static MessageWrapper FromByteArray(byte[] data, int offset, int length)
        {
            var wrapper = new MessageWrapper();
            wrapper.MessageId = (MessageId)BinarySerializer.ReadInt(data, offset); offset += 4;
            wrapper.ProtocolVersion = BinarySerializer.ReadInt(data, offset); offset += 4;
            wrapper.SequenceId = BinarySerializer.ReadLong(data, offset); offset += 8;
            int payloadLen = BinarySerializer.ReadInt(data, offset); offset += 4;

            if (payloadLen > 0)
            {
                wrapper.Payload = new byte[payloadLen];
                Array.Copy(data, offset, wrapper.Payload, 0, payloadLen);
            }

            return wrapper;
        }

        /// <summary>
        /// 从WebSocket二进制帧解析 (帧不含外层4字节长度前缀, 由WebSocket层处理)
        /// </summary>
        public static MessageWrapper ParseFrame(byte[] frameData)
        {
            return FromByteArray(frameData, 0, frameData.Length);
        }

        /// <summary>
        /// 构建包装器
        /// </summary>
        public static MessageWrapper Wrap(MessageId messageId, long sequenceId, int protocolVersion, byte[] payload)
        {
            return new MessageWrapper
            {
                MessageId = messageId,
                ProtocolVersion = protocolVersion,
                SequenceId = sequenceId,
                Payload = payload ?? Array.Empty<byte>()
            };
        }
    }
}
