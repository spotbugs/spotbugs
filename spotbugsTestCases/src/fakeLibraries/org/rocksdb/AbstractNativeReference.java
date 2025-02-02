package org.rocksdb;

public abstract class AbstractNativeReference implements AutoCloseable {
  @Override public abstract void close();
}
