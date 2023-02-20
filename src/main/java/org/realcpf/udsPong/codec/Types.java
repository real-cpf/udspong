package org.realcpf.udsPong.codec;

import io.netty.buffer.ByteBuf;

public enum Types {

  KEY_STRING((byte)'+'),
  VALUE_STRING((byte)'-'),
  COMMAND((byte)':'), // get
  GET_KEY((byte)'='), // get key
  BYTE_VALUE((byte)'.'),// byte value

  SHUT_DOWN((byte)'*');

  Types(Byte value){
  }

  public static Types from(ByteBuf in){
    return valueOf(in.readByte());
  }
  private static Types valueOf(byte b){
    return switch (b) {
      case ':' -> COMMAND;
      case '+' -> KEY_STRING;
      case '-' -> VALUE_STRING;
      case '=' -> GET_KEY;
      case '.' -> BYTE_VALUE;
      case '*' -> SHUT_DOWN;
      default -> null;
    };
  }
}
