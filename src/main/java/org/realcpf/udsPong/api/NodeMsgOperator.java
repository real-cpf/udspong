package org.realcpf.udsPong.api;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import org.realcpf.udsPong.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public final class NodeMsgOperator implements AutoCloseable {
  private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
  private final EventLoopGroup eventExecutors = new EpollEventLoopGroup();

  private String channelName;
  private Channel channel;
  private NodeMsgOperator(){}
  public void init(String name,MessageFunction messageFunction){
    DomainSocketAddress address = new DomainSocketAddress("/tmp/uds.sock");
    Bootstrap client = new Bootstrap();
    client.group(eventExecutors)
      .channel(EpollDomainSocketChannel.class)
      .handler(new ChannelInitializer<DomainSocketChannel>() {

        @Override
        protected void initChannel(DomainSocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          p.addLast(new SimpleChannelInboundHandler<Object>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
              LOGGER.info("rev msg {}",ctx.channel().id());
              String s = ((ByteBuf) msg).toString(StandardCharsets.UTF_8).trim();
              messageFunction.onMessage(s);
            }
          });
        }
      });
    try {
      channel = client.connect(address).sync().channel();
      reg(channel,name);
      this.channelName = name;
    } catch (Exception e) {
      LOGGER.error("client init error",e);
      try {
        close();
      } catch (IOException ex) {
        LOGGER.error("close event group error",ex);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    eventExecutors.shutdownGracefully();
  }

  public void sendToChannel(String channelName,String msg){
    ByteBuf buf = Unpooled.copiedBuffer(String.format(">%s %s\r\n",channelName,msg),StandardCharsets.UTF_8);
    channel.writeAndFlush(buf);
  }

  private void reg(Channel channel) {
    String id = channel.id().asShortText();
    String totalChannelName = String.format(":%s-%s\r\n",getChannelName(),id);
    reg(channel,totalChannelName);
  }
  public void unReg(){
    reg(this.channel,this.channelName);
  }
  private void reg(Channel channel,String name) {
    byte[] regData = String.format(":%s\r\n",name).getBytes(StandardCharsets.UTF_8);
    regData[0]=0x11;
    try {
      channel.writeAndFlush(Unpooled.copiedBuffer(regData)).sync();
    } catch (InterruptedException e) {
      LOGGER.error("reg channel error ",e);
      throw new RuntimeException(e);
    }
  }

  private String getChannelName(){
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LOGGER.error("get hostname error",e);
    }
    return "defaultChannel";
  }

  private static class NodeMsgOperatorInner{
    static NodeMsgOperator INSTANCE = new NodeMsgOperator();
  }

  public static NodeMsgOperator getInstance() {
    return NodeMsgOperatorInner.INSTANCE;
  }
}
