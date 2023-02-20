package before;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DemoTestByConsole {
  public static void main(String[] args) throws IOException, InterruptedException {
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
              System.out.println(msg);
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
              System.out.println("client read complate");
              super.channelReadComplete(ctx);
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
          String[] kvs = line.split(" ");
          channel.writeAndFlush(Unpooled.copiedBuffer(String.format(".%s\r\n.%s\r\n",kvs[0],kvs[1]).getBytes())).sync();
        } else {
          break;
        }
      }

    }




  }
}
