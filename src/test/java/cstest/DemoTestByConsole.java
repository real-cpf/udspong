package cstest;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class DemoTestByConsole {
  public static void main(String[] args) throws InterruptedException {
    DomainSocketAddress address = new DomainSocketAddress("/tmp/uds.sock");
    EventLoopGroup clientGroup = new EpollEventLoopGroup();

    Bootstrap client = new Bootstrap();
    client.group(clientGroup)
      .channel(EpollDomainSocketChannel.class)
      .handler(new ChannelInitializer<DomainSocketChannel>(){

        @Override
        protected void initChannel(DomainSocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          p.addLast(new SimpleChannelInboundHandler<Object>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
              System.out.println("client read");
              ByteBuf buf = (ByteBuf)msg;
              System.out.println(buf.toString(StandardCharsets.UTF_8));
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
              System.out.println("client read complate");
            }
          });
        }
      });
    Channel channel = client.connect(address).sync().channel();
    System.out.println(">");
    try(BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
      while (true){
        String line = in.readLine();
        if (null != line && !"quit".equals(line)) {
          channel.writeAndFlush(Unpooled.copiedBuffer(String.format(">two =%s\r\n",line).getBytes())).sync();
        } else {
          break;
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
