package org.realcpf.udsPong.codec;

public enum Command {

  SHUTDOWN(1),
  STATUS(2);
  Command(int c) {
    this.code = c;
  }
  private int code;
}
