package org.realcpf.udsPong.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.realcpf.udsPong.codec.KVMessage;
import org.realcpf.udsPong.codec.Message;
import org.realcpf.udsPong.codec.StringMessage;
import org.realcpf.udsPong.store.FileStoreAct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqHandler extends ChannelDuplexHandler {
  private final static Logger LOGGER = LoggerFactory.getLogger(MqHandler.class);
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof KVMessage) {
      FileStoreAct.getInstance().putByteMessage((KVMessage) msg);
      ctx.write("done");
    } else if (msg instanceof StringMessage) {
      Message message = FileStoreAct.getInstance().getMessage(((StringMessage) msg).warp());
      ctx.write(message);
    } else {
      System.out.println(msg);
      ctx.write("pong");
    }
    LOGGER.info("handler done");
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }
}
