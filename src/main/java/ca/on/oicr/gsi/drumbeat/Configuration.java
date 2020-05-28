package ca.on.oicr.gsi.drumbeat;

import java.util.Arrays;
import java.util.List;

public final class Configuration {
  private List<String> incoming;
  private int port = 8080;
  private List<Integer> splits = Arrays.asList(2, 4, 4);
  private String storage;

  public List<String> getIncoming() {
    return incoming;
  }

  public int getPort() {
    return port;
  }

  public List<Integer> getSplits() {
    return splits;
  }

  public String getStorage() {
    return storage;
  }

  public void setIncoming(List<String> incoming) {
    this.incoming = incoming;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setSplits(List<Integer> splits) {
    this.splits = splits;
  }

  public void setStorage(String storage) {
    this.storage = storage;
  }
}
