package com.rsargsyan.probarr.main_ctx.core.ports.client;

public interface ObjectStorageClient {
  void upload(String key, byte[] bytes, String contentType);
  byte[] download(String key);
}
