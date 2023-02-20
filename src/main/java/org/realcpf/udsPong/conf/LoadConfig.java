package org.realcpf.udsPong.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.logging.Level;

public final class LoadConfig {
  private LoadConfig(){
  }
  public static LoadConfig getInstance(){
    return LoadConfigInner.INSTANCE;
  }
  private static class LoadConfigInner {
    private static final LoadConfig INSTANCE = new LoadConfig();
  }
  private final static Logger LOGGER = LoggerFactory.getLogger(LoadConfig.class);
  private String dbPath;

  public String getDbPath() {
    return dbPath;
  }

  public void setDbPath(String dbPath) {
    this.dbPath = dbPath;
  }

  public String getBindPath() {
    return bindPath;
  }

  public void setBindPath(String bindPath) {
    this.bindPath = bindPath;
  }

  private String bindPath;

  public void load(String confPath){
    Properties properties = new Properties();
    try(InputStream ins = Files.newInputStream(Path.of(confPath), StandardOpenOption.READ)) {
      properties.load(ins);
    } catch (IOException e) {
      LOGGER.error("error when load config",e);
    }
    this.dbPath = properties.getProperty("dbPath",".");
    this.bindPath = properties.getProperty("bindPath",".");
    String logLevel =  properties.getProperty("logLevel","INFO");
    Level level = Level.INFO;
    if ("DEBUG".equals(logLevel)) {
      level = Level.FINE;
    }
    java.util.logging.Logger.getGlobal().setLevel(Level.ALL);

  }
}
