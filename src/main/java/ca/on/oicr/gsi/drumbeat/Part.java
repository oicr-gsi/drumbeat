package ca.on.oicr.gsi.drumbeat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Part {

  private String md5;
  private long offset;
  private int partNumber;
  private long partSize;

  @JsonInclude(Include.NON_NULL)
  private String sourceMd5;

  private String url;

  public String getMd5() {
    return md5;
  }

  public long getOffset() {
    return offset;
  }

  public int getPartNumber() {
    return partNumber;
  }

  public long getPartSize() {
    return partSize;
  }

  public String getSourceMd5() {
    return sourceMd5;
  }

  public String getUrl() {
    return url;
  }

  public void setMd5(String md5) {
    this.md5 = md5;
  }

  public void setOffset(long offset) {
    this.offset = offset;
  }

  public void setPartNumber(int partNumber) {
    this.partNumber = partNumber;
  }

  public void setPartSize(long partSize) {
    this.partSize = partSize;
  }

  public void setSourceMd5(String sourceMd5) {
    this.sourceMd5 = sourceMd5;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
