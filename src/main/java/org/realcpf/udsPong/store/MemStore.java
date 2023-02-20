package org.realcpf.udsPong.store;

import org.realcpf.udsPong.codec.ByteMessage;

import java.util.HashMap;
import java.util.Map;

public class MemStore {
  public static Map<String, ByteMessage> DB = new HashMap<>(12);
}
