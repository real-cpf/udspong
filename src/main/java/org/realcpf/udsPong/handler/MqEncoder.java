package org.realcpf.udsPong.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.realcpf.udsPong.codec.Message;

public class MqEncoder extends MessageToByteEncoder<Message> {

  @Override
  protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {

  }
}
