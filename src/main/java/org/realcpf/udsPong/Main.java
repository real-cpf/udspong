package org.realcpf.udsPong;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.realcpf.udsPong.conf.LoadConfig;
import org.realcpf.udsPong.handler.ByteMessageAggregator;
import org.realcpf.udsPong.handler.MqDecoder;
import org.realcpf.udsPong.handler.MqHandler;
import org.realcpf.udsPong.store.FileStoreAct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
  public static Path UDS_FD_PATH = Path.of("/tmp/uds.sock");
  public static Channel channel;
  public static ServerBootstrap server;
  public static EventLoopGroup eventExecutors = new EpollEventLoopGroup();
  public static EventLoopGroup eventLoopGroup = new EpollEventLoopGroup();

  public static void main(String[] args) throws InterruptedException {
    Path path;
    if (args.length == 2) {
      path = Path.of(args[1]);
    } else {
      path = UDS_FD_PATH;
    }
    start(path);
    LOGGER.info("start..on {}",path);
    LoadConfig.getInstance().load(args[0]);
    channel.closeFuture().sync();
  }

  public static void exit() {
    shutdown();
  }

  private static void shutdown() {
    eventExecutors.shutdownGracefully();
    eventLoopGroup.shutdownGracefully();
    try {
      FileStoreAct.getInstance().close();
    } catch (Exception e) {
      LOGGER.error("error when shutdown",e);
    }
  }

  private static void start(Path path) {
    try {

      if (Files.exists(path)) {
        Files.deleteIfExists(path);
      }
      server = new ServerBootstrap();
      server.group(eventExecutors, eventLoopGroup)
        .channel(EpollServerDomainSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.DEBUG))
        .childHandler(new ChannelInitializer<DomainSocketChannel>() {
          @Override
          protected void initChannel(DomainSocketChannel ch) {
            if (path.toString().contains("two")) {
              LOGGER.info("{}",ch.id());
            }
            ChannelPipeline p = ch.pipeline();
            p.addLast(new MqDecoder());
            p.addLast(new ByteMessageAggregator());
            p.addLast(new MqHandler());
          }
        });
      channel = server.bind(new DomainSocketAddress(path.toString())).sync().channel();
    } catch (Exception e) {
      LOGGER.error("error when bind path:",e);
    }
  }
}
