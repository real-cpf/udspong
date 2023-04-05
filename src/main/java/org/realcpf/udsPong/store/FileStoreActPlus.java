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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.netty.util.ByteProcessor.FIND_NUL;
import static io.netty.util.ByteProcessor.FIND_SEMI_COLON;

public final class FileStoreActPlus implements AutoCloseable {
  private final static Logger LOGGER = LoggerFactory.getLogger(FileStoreActPlus.class);

  @Override
  public void close() throws Exception {
    tool.close();
  }

  public void putByteMessage(KVMessage message) {
    tool.pushByteMessage(message);
  }

  private static class MateKey {
    public MateKey(long start, long len) {
      this.start = start;
      this.len = len;
    }

    long start;
    long len;
  }

  public Message getMessage(String key) {
    return tool.getMessage(key);
  }

  private static class FileChannelTool {
    private final long MAX_LEN = 10 * 1024 * 1024;
    private final String KEY_DB_FILE_TEMPLATE = "db.key";
    private final String DATA_DB_FILE_TEMPLATE = "db.data";

    private final int MAX_INC_OFFSET = 16;
    private volatile int fileMapArrayOffset = -1;
    private final StandardOpenOption[] INTI_OPTIONS = new StandardOpenOption[]{
      StandardOpenOption.WRITE, StandardOpenOption.READ
    };
    private final FileChannel.MapMode DEFAULT_MODE = FileChannel.MapMode.READ_WRITE;
    private final FileChannel[] keyFileChannelArray = new FileChannel[MAX_INC_OFFSET];
    private final FileChannel[] dataFileChannelArray = new FileChannel[MAX_INC_OFFSET];
    private final ByteBuf[] dataByteBufArray = new ByteBuf[MAX_INC_OFFSET];
    private final ByteBuf[] keyByteBufArray = new ByteBuf[MAX_INC_OFFSET];

    private static Map<String, MateKey> map;

    private FileChannelTool() {

      try {
        Path basePath = Path.of(LoadConfig.getInstance().getDbPath());
        List<Path> keyFiles = new LinkedList<>();
        List<Path> dataFiles = new LinkedList<>();
        Files.list(basePath).filter(f -> {
          String name = f.getFileName().toString();
          return name.startsWith(KEY_DB_FILE_TEMPLATE) || name.startsWith(DATA_DB_FILE_TEMPLATE);
        }).sorted().forEach(e -> {
          String name = e.getFileName().toString();
          if (name.startsWith(KEY_DB_FILE_TEMPLATE)) {
            keyFiles.add(e);
          } else if (name.startsWith(DATA_DB_FILE_TEMPLATE)) {
            dataFiles.add(e);
          }
        });
        if (keyFiles.size() != dataFiles.size()) {
          throw new RuntimeException("wrong file pair");
        }

        for (int i = 0; i < keyFiles.size(); i++) {
          keyFileChannelArray[i] = FileChannel.open(keyFiles.get(i), INTI_OPTIONS);
          keyByteBufArray[i] = Unpooled.wrappedBuffer(keyFileChannelArray[i].map(DEFAULT_MODE, 0, MAX_LEN));
          dataFileChannelArray[i] = FileChannel.open(dataFiles.get(i), INTI_OPTIONS);
          dataByteBufArray[i] = Unpooled.wrappedBuffer(dataFileChannelArray[i].map(DEFAULT_MODE, 0, MAX_LEN));
          fileMapArrayOffset = i;
        }
        if (fileMapArrayOffset == -1) {

          Path kPath = basePath.resolve(KEY_DB_FILE_TEMPLATE);
          Path dPath = basePath.resolve(DATA_DB_FILE_TEMPLATE);
          if (!Files.exists(kPath)) {
            Files.createFile(kPath);
            Files.createFile(dPath);
          }
          keyFileChannelArray[fileMapArrayOffset] = FileChannel.open(kPath, INTI_OPTIONS);
          keyByteBufArray[fileMapArrayOffset] = Unpooled.wrappedBuffer(keyFileChannelArray[fileMapArrayOffset].map(DEFAULT_MODE, 0, MAX_LEN));
          dataFileChannelArray[fileMapArrayOffset] = FileChannel.open(dPath, INTI_OPTIONS);
          dataByteBufArray[fileMapArrayOffset] = Unpooled.wrappedBuffer(dataFileChannelArray[fileMapArrayOffset].map(DEFAULT_MODE, 0, MAX_LEN));
        }

        firstLoad();
      } catch (IOException e) {
        LOGGER.error("error when <init> FileStoreAct", e);
      }
    }

    private void firstLoad() {
      map = new HashMap<>();
      int offset = fileMapArrayOffset;
      for (int i = 0; i <= offset; i++) {
        int index;
        int last = 0;
        int end = keyByteBufArray[i].forEachByte(FIND_NUL);
        if (end == -1) {
          return;
        }
        ByteBuf byteBuf = keyByteBufArray[i].slice(0, end);
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
          LOGGER.info("load key from file {}", keyName);
          map.put(keyName, new MateKey(start, len));
          byteBuf.readByte();
          last = index + 1;
        }
      }

      int di = dataByteBufArray[fileMapArrayOffset].forEachByte(ByteProcessor.FIND_NUL);
      dataByteBufArray[fileMapArrayOffset].writerIndex(di == -1 ? 0 : di);
      int ki = keyByteBufArray[fileMapArrayOffset].forEachByte(ByteProcessor.FIND_NUL);
      keyByteBufArray[fileMapArrayOffset].writerIndex(ki == -1 ? 0 : ki);
    }

    private static final FileStoreActPlus act = new FileStoreActPlus();


    public Message getMessage(String key) {
      MateKey mateKey = map.get(key);
      if (null == mateKey) {
        return null;
      }
      long len = mateKey.len;
      long start = mateKey.start;
      dataByteBufArray[fileMapArrayOffset].readerIndex((int) start);
      ByteBuf buf = Unpooled.buffer((int) len);
      dataByteBufArray[fileMapArrayOffset].readBytes(buf, (int) len);
      return new ByteMessage(buf);
    }


    public int pushByteMessage(KVMessage message) {
      synchronized (act) {
        ByteMessage keyMsg = (ByteMessage) message.getKey();
        ByteMessage valueMsg = (ByteMessage) message.getValue();
        ByteBuf keyBuf = keyMsg.warp();
        ByteBuf valueBuf = valueMsg.warp();

        String kName = keyBuf.toString(StandardCharsets.UTF_8);
        LOGGER.debug("add value with key {}", kName);
        map.put(kName, new MateKey(dataByteBufArray[fileMapArrayOffset].writerIndex(), valueBuf.readableBytes()));

        ByteBuf keyHead = Unpooled.copiedBuffer(String.format("%s:%s:", dataByteBufArray[fileMapArrayOffset].writerIndex(), valueBuf.readableBytes()), StandardCharsets.UTF_8);
        dataByteBufArray[fileMapArrayOffset].writeBytes(valueBuf);
        byte[] bytes = new byte[1];
        bytes[0] = ';';
        ByteBuf byteBuf = Unpooled.wrappedBuffer(keyHead, keyBuf, Unpooled.copiedBuffer(bytes));
        keyByteBufArray[fileMapArrayOffset].writeBytes(byteBuf);
      }
      return 1;
    }

    public void close() throws IOException {
      for (int i = 0; i <= fileMapArrayOffset; i++) {
        keyFileChannelArray[i].close();
        dataFileChannelArray[i].close();
      }

    }

  }

  private final FileChannelTool tool;

  private FileStoreActPlus() {
    tool = new FileChannelTool();
  }

  public static FileStoreActPlus getInstance() {
    return FileChannelTool.act;
  }
}
