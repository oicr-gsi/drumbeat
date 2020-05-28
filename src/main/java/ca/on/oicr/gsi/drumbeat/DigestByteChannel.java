package ca.on.oicr.gsi.drumbeat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class DigestByteChannel implements WritableByteChannel {
  private final MessageDigest digest;
  private final WritableByteChannel inner;

  DigestByteChannel(WritableByteChannel inner) throws NoSuchAlgorithmException {
    this.inner = inner;
    digest = MessageDigest.getInstance("MD5");
  }

  @Override
  public void close() throws IOException {
    inner.close();
  }

  public String digest() {
    final var builder = new StringBuilder();
    for (final var b : digest.digest()) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString();
  }

  @Override
  public boolean isOpen() {
    return inner.isOpen();
  }

  @Override
  public int write(ByteBuffer byteBuffer) throws IOException {
    digest.update(byteBuffer);
    return inner.write(byteBuffer);
  }
}
