package org.realcpf.udsPong.node;


import io.netty.channel.Channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;

public final class NodeConf {
  private NodeConf(){}

  private final static Map<String, Channel> channelMap = new ConcurrentSkipListMap<>();

  public void putChannel(String name,Channel channel) {
    channelMap.put(name,channel);
  }
  public Optional<Channel> getChannel(String name){
    if (channelMap.containsKey(name)) {
      return Optional.of(channelMap.get(name));
    }
    return Optional.empty();
  }

  public static NodeConf getInstance() {
    return NodeConfInner.INSTANCE;
  }

  private static class NodeConfInner {
    static NodeConf INSTANCE = new NodeConf();
  }
}
