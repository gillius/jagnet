package org.gillius.jagnet.netty;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.List;

public class KryoDecoder extends ByteToMessageDecoder {
	private final Kryo kryo;
	private final ByteBufferInput input;

	public KryoDecoder(Kryo kryo) {
		this.kryo = kryo;
		input = new ByteBufferInput();
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		ByteBuffer nioBuf = in.nioBuffer();
		input.setBuffer(nioBuf);
		out.add(kryo.readClassAndObject(input));
		in.skipBytes(nioBuf.position());
	}
}
