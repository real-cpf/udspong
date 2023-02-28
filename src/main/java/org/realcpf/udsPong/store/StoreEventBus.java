package org.realcpf.udsPong.store;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.realcpf.udsPong.api.MessageEvent;
import org.realcpf.udsPong.api.MessageStoreHandler;
import org.realcpf.udsPong.codec.Message;

public final class StoreEventBus {
  private final Disruptor<MessageEvent> disruptor;
  private final RingBuffer<MessageEvent> ringBuffer;

  private static class StoreEventBusInner {
    static StoreEventBus INSTANCE = new StoreEventBus();
  }

  private StoreEventBus() {
    int bufferSize = 1024;
    disruptor = new Disruptor<>(
      MessageEvent::new, bufferSize, DaemonThreadFactory.INSTANCE
    );
    disruptor.handleEventsWith(new MessageStoreHandler());
    disruptor.start();
    ringBuffer = disruptor.getRingBuffer();

  }
  public static StoreEventBus getInstance() {
    return StoreEventBusInner.INSTANCE;
  }

  public void writeMessage(Message message) {
    ringBuffer.publishEvent((event, sequence) -> {
      event.setMessage(message);
    });
  }


}
