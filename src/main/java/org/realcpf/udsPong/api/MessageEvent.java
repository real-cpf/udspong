package org.realcpf.udsPong.api;

import org.realcpf.udsPong.codec.Message;

public class MessageEvent {
  private Message message;
  public void setMessage(Message message) {
    this.message = message;
  }

  public Message getMessage() {
    return message;
  }
}
