package ca.on.oicr.gsi.drumbeat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ObjectSpecification {

  private String objectId;
  private String objectKey;
  private String objectMd5;
  private long objectSize;
  private List<Part> parts;
  private String uploadId;

  public String getObjectId() {
    return objectId;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getObjectMd5() {
    return objectMd5;
  }

  public long getObjectSize() {
    return objectSize;
  }

  public List<Part> getParts() {
    return parts;
  }

  public String getUploadId() {
    return uploadId;
  }

  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  public void setObjectKey(String objectKey) {
    this.objectKey = objectKey;
  }

  public void setObjectMd5(String objectMd5) {
    this.objectMd5 = objectMd5;
  }

  public void setObjectSize(long objectSize) {
    this.objectSize = objectSize;
  }

  public void setParts(List<Part> parts) {
    this.parts = parts;
  }

  public void setUploadId(String uploadId) {
    this.uploadId = uploadId;
  }
}
