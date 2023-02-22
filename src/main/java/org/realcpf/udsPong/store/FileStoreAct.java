package org.realcpf.udsPong.store;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import org.realcpf.udsPong.codec.ByteMessage;
import org.realcpf.udsPong.codec.KVMessage;
import org.realcpf.udsPong.codec.Message;
import org.realcpf.udsPong.conf.LoadConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static io.netty.util.ByteProcessor.FIND_NUL;
import static io.netty.util.ByteProcessor.FIND_SEMI_COLON;

public final class FileStoreAct implements AutoCloseable {
  private final static Logger LOGGER = LoggerFactory.getLogger(FileStoreAct.class);

  @Override
  public void close() throws Exception {
    tool.close();
  }

  public void putByteMessage(KVMessage message) {
    tool.pushByteMessage(message);
  }

  private static class MateKey {
    public MateKey(long start,long len){
      this.start = start;
      this.len = len;
    }
    long start;
    long len;
  }

  public Message getMessage(String key){
    return tool.getMessage(key);
  }

  private static class FileChannelTool {
    private final long MAX_LEN = 10 * 1024 * 1024;
    private FileChannel keyFileChannel;
    private FileChannel dataFileChannel;

    private ByteBuf dataByteBuf;
    private ByteBuf keyByteBuf;
    private static Map<String, MateKey> map;

    private FileChannelTool() {

      try {
        Path basePath = Path.of(LoadConfig.getInstance().getDbPath());

        Path kPath = basePath.resolve("db.key");
        Path dPath = basePath.resolve("db.data");
        if (!Files.exists(kPath)) {
          Files.createFile(kPath);
          Files.createFile(dPath);
        }
        keyFileChannel = FileChannel.open(kPath, StandardOpenOption.WRITE, StandardOpenOption.READ);
        dataFileChannel = FileChannel.open(dPath, StandardOpenOption.WRITE, StandardOpenOption.READ);
        MappedByteBuffer keyFileMapped = keyFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_LEN);
        MappedByteBuffer dataFileMapped = dataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, MAX_LEN);
        dataByteBuf = Unpooled.wrappedBuffer(dataFileMapped);
        keyByteBuf = Unpooled.wrappedBuffer(keyFileMapped);
        firstLoad();
      } catch (IOException e) {
        LOGGER.error("error when <init> FileStoreAct",e);
      }
    }

    private void firstLoad() {
      map = new HashMap<>();
      int index;
      int last = 0;
      int end = keyByteBuf.forEachByte(FIND_NUL);
      if (end == -1) {
        return;
      }
      ByteBuf byteBuf = keyByteBuf.slice(0, end);
      for (; ; ) {
        index = byteBuf.forEachByte(FIND_SEMI_COLON);
        if (index < 0) {
          break;
        }
        String line = byteBuf.readSlice(index - last).toString(StandardCharsets.UTF_8);
        String[] lines = line.split(":");
        long start = Long.parseLong(lines[0]);
        long len = Long.parseLong(lines[1]);
        String keyName = lines[2];
        LOGGER.info("load key from file {}",keyName);
        map.put(keyName,new MateKey(start,len));
        byteBuf.readByte();
        last = index + 1;
      }
      int di = dataByteBuf.forEachByte(ByteProcessor.FIND_NUL);
      dataByteBuf.writerIndex(di == -1 ? 0 : di);
      int ki = keyByteBuf.forEachByte(ByteProcessor.FIND_NUL);
      keyByteBuf.writerIndex(ki == -1 ? 0 : ki);
    }

    private static final FileStoreAct act = new FileStoreAct();


    public Message getMessage(String key) {
      MateKey mateKey = map.get(key);
      if (null == mateKey) {
        return null;
      }
      long len = mateKey.len;
      long start = mateKey.start;
      dataByteBuf.readerIndex((int) start);
      ByteBuf buf = Unpooled.buffer((int) len);
      dataByteBuf.readBytes(buf, (int) len);
      return new ByteMessage(buf);
    }


    public int pushByteMessage(KVMessage message) {
      synchronized (act){
        ByteMessage keyMsg = (ByteMessage) message.getKey();
        ByteMessage valueMsg = (ByteMessage) message.getValue();
        ByteBuf keyBuf = keyMsg.warp();
        ByteBuf valueBuf = valueMsg.warp();

        String kName = keyBuf.toString(StandardCharsets.UTF_8);
        LOGGER.debug("add value with key {}",kName);
        map.put(kName,new MateKey(dataByteBuf.writerIndex(),valueBuf.readableBytes()));

        ByteBuf keyHead = Unpooled.copiedBuffer(String.format("%s:%s:", dataByteBuf.writerIndex(), valueBuf.readableBytes()), StandardCharsets.UTF_8);
        dataByteBuf.writeBytes(valueBuf);
        byte[] bytes = new byte[1];
        bytes[0] = ';';
        ByteBuf byteBuf = Unpooled.wrappedBuffer(keyHead, keyBuf, Unpooled.copiedBuffer(bytes));
        keyByteBuf.writeBytes(byteBuf);
      }
      return 1;
    }
    public void close() throws IOException {
      this.keyFileChannel.close();
      this.dataFileChannel.close();

    }

  }

  private final FileChannelTool tool;

  private FileStoreAct() {
    tool = new FileChannelTool();
  }

  public static FileStoreAct getInstance() {
    return FileChannelTool.act;
  }
}
