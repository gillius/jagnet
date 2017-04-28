package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class KryoEncoder extends MessageToByteEncoder<Object> {
	private final Kryo kryo;
	private final boolean includeLengthField;
	private final Output output;

	public KryoEncoder(Kryo kryo, boolean includeLengthField) {
		this.kryo = kryo;
		this.includeLengthField = includeLengthField;
		output = new Output(1024, 65535);
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		output.clear();
		kryo.writeClassAndObject(output, msg);
		int len = output.position();
		if (includeLengthField)
			out.writeShort(len);
		out.writeBytes(output.getBuffer(), 0, len);
	}
}
