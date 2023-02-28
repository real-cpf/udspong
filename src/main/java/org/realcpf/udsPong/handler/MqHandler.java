package org.realcpf.udsPong.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.realcpf.udsPong.Center;
import org.realcpf.udsPong.codec.*;
import org.realcpf.udsPong.node.NodeConf;
import org.realcpf.udsPong.store.FileStoreAct;
import org.realcpf.udsPong.store.StoreEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

public class MqHandler extends ChannelDuplexHandler {
  private final static Logger LOGGER = LoggerFactory.getLogger(MqHandler.class);
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof KVMessage) {
      StoreEventBus.getInstance().writeMessage((Message) msg);
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
      processCommand(ctx, command);
    } else {
      System.out.println(msg);
      ctx.write("pong");
    }
    LOGGER.info("handler done");
  }

  private static void processCommand(ChannelHandlerContext ctx, Command command) {
    switch (command) {
      case CLOSE_CONNECTION -> {
        ctx.close().addListener(future -> {
          if (future.isDone()){
            ctx.fireChannelRegistered();
          }
        });
      }
      case SHUT_DOWN -> {
        ctx.close().addListener(e->{
          if (e.isDone()){
            NodeConf.getInstance().closeAll();
          }
        });

        Center.exit();
      }
    }
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
