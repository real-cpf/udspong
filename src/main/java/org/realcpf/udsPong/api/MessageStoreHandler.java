package org.realcpf.udsPong.api;

import com.lmax.disruptor.EventHandler;
import org.realcpf.udsPong.codec.KVMessage;
import org.realcpf.udsPong.handler.MqHandler;
import org.realcpf.udsPong.store.FileStoreAct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageStoreHandler implements EventHandler<MessageEvent> {
  private final static Logger LOGGER = LoggerFactory.getLogger(MessageStoreHandler.class);
  @Override
  public void onEvent(MessageEvent event, long sequence, boolean endOfBatch) throws Exception {
    LOGGER.info("on event {}",sequence);
    FileStoreAct.getInstance().putByteMessage((KVMessage) event.getMessage());
  }

}
