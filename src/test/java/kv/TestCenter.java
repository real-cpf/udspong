package kv;

import java.nio.file.Path;

import static org.realcpf.udsPong.Center.start;

public class TestCenter {
  public static void main(String[] args) throws InterruptedException {
    Path path;
    if (args.length == 2) {
      path = Path.of(args[1]);
    } else {
      path = Path.of("/tmp/uds.sock");
    }
    start(path,"./src/main/resources/config.prop");
  }
}
