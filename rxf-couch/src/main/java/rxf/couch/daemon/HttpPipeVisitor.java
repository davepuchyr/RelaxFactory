package rxf.couch.daemon;

import one.xio.AsioVisitor;
import rxf.core.Config;
import rxf.shared.PreRead;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

/**
 * this visitor shovels data from the outward selector to the inward selector, and vice versa. once the headers are sent
 * inward the only state monitored is when one side of the connections close.
 */
@PreRead
public class HttpPipeVisitor extends AsioVisitor.Impl {
  public static final boolean PROXY_DEBUG = "true".equals(Config.get("PROXY_DEBUG", String
      .valueOf(false)));
  final private ByteBuffer[] b;
  protected String name;
  // public AtomicInteger remaining;
  SelectionKey otherKey;
  private boolean limit;

  public HttpPipeVisitor(String name, SelectionKey otherKey, ByteBuffer... b) {
    this.name = name;
    this.otherKey = otherKey;
    this.b = b;
  }

  public void onRead(SelectionKey key) throws Exception {
    SocketChannel channel = (SocketChannel) key.channel();
    if (otherKey.isValid()) {
      int read = Helper.read(key, getInBuffer());
      if (read == -1) /* key.cancel(); */{
        channel.shutdownInput();
        key.interestOps(OP_WRITE);
        Helper.write(key, ByteBuffer.allocate(0));
      } else {
        // if buffer fills up, stop the read option for a bit
        otherKey.interestOps(OP_READ | OP_WRITE);
        Helper.write(key, ByteBuffer.allocate(0));
      }
    } else {
      key.cancel();
    }
  }

  public void onWrite(SelectionKey key) throws Exception {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer flip = (ByteBuffer) getOutBuffer().flip();
    if (PROXY_DEBUG) {
      CharBuffer decode = StandardCharsets.UTF_8.decode(flip.duplicate());
      System.err.println("writing to " + name + ": " + decode + "-");
    }
    int write = channel.write(flip);

    if (-1 == write || isLimit() /* && null != remaining && 0 == remaining.get() */) {
      key.cancel();
    } else {
      // if (isLimit() /*&& null != remaining*/) {
      // /*this.remaining.getAndAdd(-write);*//*
      // if (1 > remaining.get()) */{
      // key.channel().close();
      // otherKey.channel().close();
      // return;
      // }
      // }
      key.interestOps(OP_READ | OP_WRITE);// (getOutBuffer().hasRemaining() ? OP_WRITE : 0));
      getOutBuffer().compact();
    }
  }

  public ByteBuffer getInBuffer() {
    return b[0];
  }

  public ByteBuffer getOutBuffer() {
    return b[1];
  }

  public boolean isLimit() {
    return limit;
  }

  public void setLimit(boolean limit) {
    this.limit = limit;
  }
}
