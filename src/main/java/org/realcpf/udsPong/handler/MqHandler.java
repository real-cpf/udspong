package org.realcpf.udsPong.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.unix.DomainSocketAddress;
import org.realcpf.udsPong.codec.*;
import org.realcpf.udsPong.store.FileStoreAct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class MqHandler extends ChannelDuplexHandler {
  private final static Logger LOGGER = LoggerFactory.getLogger(MqHandler.class);
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof KVMessage) {
      FileStoreAct.getInstance().putByteMessage((KVMessage) msg);
      ctx.write(Unpooled.copiedBuffer("done".getBytes()));
    } else if (msg instanceof StringMessage stringMessage) {
      String s = stringMessage.warp();
      LOGGER.info("rev {}",s);
      Message message = FileStoreAct.getInstance().getMessage(s);
      if (null != message) {
        ctx.write(Unpooled.copiedBuffer(((StringMessage)message).warp().getBytes()));
      } else {
        LOGGER.info("can not found {} in db file",s);
      }

    } else if(msg instanceof CommandMessage commandMessage){
      Command command = commandMessage.getCommand();
      LOGGER.info("get command from {}",ctx.channel().id());
      switch (command) {
        case SHUTDOWN -> {
          ctx.close().addListener(future -> {
            if (future.isDone()){
              ctx.fireChannelRegistered();
            }
          });
        }
      }
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

  @Override
  public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
    super.connect(ctx, remoteAddress, localAddress, promise);
  }
}
