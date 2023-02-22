package route;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.realcpf.udsPong.Main;
import org.realcpf.udsPong.conf.LoadConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class TestRoute {

  private static Set<String> sendValues = new HashSet<>(12);
  private static DomainSocketAddress address = new DomainSocketAddress("/tmp/uds.sock");
  private static EventLoopGroup clientGroup = new EpollEventLoopGroup();

  private Channel initClient() throws InterruptedException {
    Bootstrap client = new Bootstrap();
    client.group(clientGroup)
      .channel(EpollDomainSocketChannel.class)
      .handler(new ChannelInitializer<DomainSocketChannel>() {

        @Override
        protected void initChannel(DomainSocketChannel ch) throws Exception {
          ChannelPipeline p = ch.pipeline();
          p.addLast(new SimpleChannelInboundHandler<Object>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
              System.out.printf("%n[Test]client read");
              String s = ((ByteBuf) msg).toString(StandardCharsets.UTF_8).trim();
              if (s.startsWith("=")) {
                Assertions.assertTrue(sendValues.remove(s),s);

              }
              System.out.println(s);
            }

            @Override
            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
              System.out.println("[Test]client read complete");
            }
          });
        }
      });
    return client.connect(address).sync().channel();
  }


  @Test
  public void run() throws InterruptedException {


    LoadConfig.getInstance().load(Path.of("./src/main/resources/config.prop").toAbsolutePath().toString());
    Channel centerChannel = Main.start(Path.of(address.path()));
    assert centerChannel != null;
    Channel regChannel = initClient();
    Channel senderChannel = initClient();

    System.out.println(regChannel.id());
    System.out.println(senderChannel.id());
    regChannel.writeAndFlush(regMsg("abc")).await();
    for (int i=0;i<10;i++){
      sendValues.add(String.format("=routeMsg-%d",i));
      TimeUnit.MILLISECONDS.sleep(256);
      senderChannel.writeAndFlush(Unpooled.copiedBuffer(
        String.format(">abc =%s-%d\r\n","routeMsg",i).getBytes(StandardCharsets.UTF_8))).await();

    }

    centerChannel.closeFuture().await(500, TimeUnit.MILLISECONDS);
    Main.exit();
    clientGroup.shutdownGracefully();
    Assertions.assertEquals(0,sendValues.size());
  }

  private ByteBuf regMsg(String name){
    byte[] regFlag = new byte[1];
    regFlag[0] = 0x11;
    byte[] msgEnd = new byte[2];
    msgEnd[0] = '\r';
    msgEnd[1] = '\n';
    return Unpooled.wrappedBuffer(regFlag,name.getBytes(StandardCharsets.UTF_8),msgEnd);
  }
}
