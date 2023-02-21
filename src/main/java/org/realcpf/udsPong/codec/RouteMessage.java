package org.realcpf.udsPong.codec;

public class RouteMessage implements Message{
  private final String routeKey;

  public String getRouteKey() {
    return routeKey;
  }

  public Message getMessage() {
    return message;
  }

  private final Message message;
  public RouteMessage(String routeKey,Message message) {
    this.routeKey = routeKey;
    this.message = message;
  }
  @Override
  public long length() {
    return 0;
  }
}
