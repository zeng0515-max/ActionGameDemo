using System;
using NUnit.Framework;
using ActionGameDemo.Protocol;

namespace ActionGameDemo.Tests
{
    /// <summary>
    /// BinarySerializer 序列化/反序列化往返测试
    /// </summary>
    [TestFixture]
    public class BinarySerializerTests
    {
        [Test]
        public void Int_RoundTrip()
        {
            byte[] buf = new byte[4];
            int[] values = { 0, 1, -1, 255, 65535, int.MaxValue, int.MinValue, 42 };
            foreach (var v in values)
            {
                BinarySerializer.WriteInt(buf, 0, v);
                Assert.AreEqual(v, BinarySerializer.ReadInt(buf, 0), $"Int round-trip failed for {v}");
            }
        }

        [Test]
        public void Long_RoundTrip()
        {
            byte[] buf = new byte[8];
            long[] values = { 0L, 1L, -1L, long.MaxValue, long.MinValue, 999999999999L };
            foreach (var v in values)
            {
                BinarySerializer.WriteLong(buf, 0, v);
                Assert.AreEqual(v, BinarySerializer.ReadLong(buf, 0), $"Long round-trip failed for {v}");
            }
        }

        [Test]
        public void Float_RoundTrip()
        {
            byte[] buf = new byte[4];
            float[] values = { 0f, 1f, -1f, 3.14159f, float.MaxValue, float.MinValue, float.Epsilon };
            foreach (var v in values)
            {
                BinarySerializer.WriteFloat(buf, 0, v);
                float result = BinarySerializer.ReadFloat(buf, 0);
                Assert.AreEqual(v, result, 0.0001f, $"Float round-trip failed for {v}");
            }
        }

        [Test]
        public void Bool_RoundTrip()
        {
            byte[] buf = new byte[1];
            BinarySerializer.WriteBool(buf, 0, true);
            Assert.IsTrue(BinarySerializer.ReadBool(buf, 0));
            BinarySerializer.WriteBool(buf, 0, false);
            Assert.IsFalse(BinarySerializer.ReadBool(buf, 0));
        }

        [Test]
        public void String_RoundTrip()
        {
            string[] values = { "", "Hello", "你好世界", "Test123!@#" };
            foreach (var v in values)
            {
                int size = BinarySerializer.StringSize(v);
                byte[] buf = new byte[size];
                BinarySerializer.WriteString(buf, 0, v, 256);
                Assert.AreEqual(v, BinarySerializer.ReadString(buf, 0), $"String round-trip failed for '{v}'");
            }
        }

        [Test]
        public void String_Empty_ReturnsEmpty()
        {
            Assert.AreEqual(string.Empty, BinarySerializer.ReadString(new byte[4], 0));
            Assert.AreEqual(4, BinarySerializer.StringSize(null));
            Assert.AreEqual(4, BinarySerializer.StringSize(""));
        }

        [Test]
        public void String_TruncatesToMaxBytes()
        {
            string longStr = new string('A', 100);
            int maxBytes = 10;
            int size = BinarySerializer.StringSize(longStr);
            byte[] buf = new byte[size];
            BinarySerializer.WriteString(buf, 0, longStr, maxBytes);
            string result = BinarySerializer.ReadString(buf, 0);
            Assert.AreEqual(maxBytes, result.Length, "String should be truncated to maxBytes");
        }

        [Test]
        public void MultiField_SequentialRead()
        {
            byte[] buf = new byte[4 + 8 + 4 + 20];
            int offset = 0;
            BinarySerializer.WriteInt(buf, offset, 42); offset += 4;
            BinarySerializer.WriteLong(buf, offset, 123456789L); offset += 8;
            BinarySerializer.WriteInt(buf, offset, -7); offset += 4;
            BinarySerializer.WriteString(buf, offset, "Test", 256); offset += BinarySerializer.StringSize("Test");

            offset = 0;
            Assert.AreEqual(42, BinarySerializer.ReadInt(buf, offset)); offset += 4;
            Assert.AreEqual(123456789L, BinarySerializer.ReadLong(buf, offset)); offset += 8;
            Assert.AreEqual(-7, BinarySerializer.ReadInt(buf, offset)); offset += 4;
            Assert.AreEqual("Test", BinarySerializer.ReadString(buf, offset));
        }
    }

    /// <summary>
    /// MessageWrapper 包装/解析往返测试
    /// </summary>
    [TestFixture]
    public class MessageWrapperTests
    {
        [Test]
        public void WrapAndParse_RoundTrip()
        {
            byte[] payload = { 1, 2, 3, 4, 5 };
            var wrapper = MessageWrapper.Wrap(MessageId.LoginReq, 42L, 10, payload);
            byte[] data = wrapper.ToByteArray();

            var parsed = MessageWrapper.ParseFrame(data);
            Assert.AreEqual(MessageId.LoginReq, parsed.MessageId);
            Assert.AreEqual(10, parsed.ProtocolVersion);
            Assert.AreEqual(42L, parsed.SequenceId);
            Assert.AreEqual(payload.Length, parsed.Payload.Length);
            for (int i = 0; i < payload.Length; i++)
                Assert.AreEqual(payload[i], parsed.Payload[i]);
        }

        [Test]
        public void Wrap_EmptyPayload()
        {
            var wrapper = MessageWrapper.Wrap(MessageId.HeartbeatReq, 1L, 10, null);
            byte[] data = wrapper.ToByteArray();
            var parsed = MessageWrapper.ParseFrame(data);

            Assert.AreEqual(MessageId.HeartbeatReq, parsed.MessageId);
            Assert.AreEqual(1L, parsed.SequenceId);
            Assert.IsNotNull(parsed.Payload);
            Assert.AreEqual(0, parsed.Payload.Length);
        }

        [Test]
        public void Wrap_LargePayload()
        {
            byte[] payload = new byte[4096];
            new Random(42).NextBytes(payload);
            var wrapper = MessageWrapper.Wrap(MessageId.BattleFrameNotify, 999L, 10, payload);
            byte[] data = wrapper.ToByteArray();
            var parsed = MessageWrapper.ParseFrame(data);

            Assert.AreEqual(MessageId.BattleFrameNotify, parsed.MessageId);
            Assert.AreEqual(999L, parsed.SequenceId);
            Assert.AreEqual(payload.Length, parsed.Payload.Length);
            for (int i = 0; i < payload.Length; i++)
                Assert.AreEqual(payload[i], parsed.Payload[i]);
        }

        [Test]
        public void HeaderSize_IsConstant()
        {
            var wrapper = new MessageWrapper();
            Assert.AreEqual(20, wrapper.HeaderSize);
        }

        [Test]
        public void MultiMessage_SequentialParse()
        {
            byte[] payload1 = { 0xAA, 0xBB };
            byte[] payload2 = { 0xCC, 0xDD, 0xEE };

            var w1 = MessageWrapper.Wrap(MessageId.LoginReq, 1L, 10, payload1);
            var w2 = MessageWrapper.Wrap(MessageId.LoginResp, 2L, 10, payload2);

            byte[] data1 = w1.ToByteArray();
            byte[] data2 = w2.ToByteArray();

            var p1 = MessageWrapper.ParseFrame(data1);
            var p2 = MessageWrapper.ParseFrame(data2);

            Assert.AreEqual(MessageId.LoginReq, p1.MessageId);
            Assert.AreEqual(1L, p1.SequenceId);
            Assert.AreEqual(MessageId.LoginResp, p2.MessageId);
            Assert.AreEqual(2L, p2.SequenceId);
        }

        [Test]
        public void MaxValues_RoundTrip()
        {
            var wrapper = MessageWrapper.Wrap(
                MessageId.BattleEndNotify, long.MaxValue, int.MaxValue, new byte[] { 0xFF });
            byte[] data = wrapper.ToByteArray();
            var parsed = MessageWrapper.ParseFrame(data);

            Assert.AreEqual(MessageId.BattleEndNotify, parsed.MessageId);
            Assert.AreEqual(long.MaxValue, parsed.SequenceId);
            Assert.AreEqual(int.MaxValue, parsed.ProtocolVersion);
        }
    }

    /// <summary>
    /// JoinRoomResp 重定向字段向后兼容测试
    /// </summary>
    [TestFixture]
    public class JoinRoomRespTests
    {
        [Test]
        public void RoundTrip_WithRedirectFields()
        {
            var resp = new JoinRoomResp
            {
                ProtocolVersion = 10,
                Code = -3,
                RoomId = "room-1",
                PlayerEntityId = -1,
                StartFrameIndex = 0,
                RedirectNodeId = "node-2",
                RedirectAddress = "game-2:9090"
            };

            var parsed = new JoinRoomResp();
            parsed.FromByteArray(resp.ToByteArray());

            Assert.AreEqual(-3, parsed.Code);
            Assert.AreEqual("node-2", parsed.RedirectNodeId);
            Assert.AreEqual("game-2:9090", parsed.RedirectAddress);
        }

        [Test]
        public void Parse_OldPayloadWithoutRedirectFields()
        {
            byte[] payload = new byte[4 + 4 + BinarySerializer.StringSize("room-1") + 4 + 4];
            int offset = 0;
            BinarySerializer.WriteInt(payload, offset, 10); offset += 4;
            BinarySerializer.WriteInt(payload, offset, 0); offset += 4;
            BinarySerializer.WriteString(payload, offset, "room-1", 64); offset += BinarySerializer.StringSize("room-1");
            BinarySerializer.WriteInt(payload, offset, 7); offset += 4;
            BinarySerializer.WriteInt(payload, offset, 0);

            var parsed = new JoinRoomResp();
            parsed.FromByteArray(payload);

            Assert.AreEqual(7, parsed.PlayerEntityId);
            Assert.AreEqual(string.Empty, parsed.RedirectNodeId);
            Assert.AreEqual(string.Empty, parsed.RedirectAddress);
        }
    }
}
