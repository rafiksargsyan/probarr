package com.rsargsyan.probarr.main_ctx.adapters.driven;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

@Slf4j
public class TorrentHashResolver {

  private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  public static String extractFromMagnet(String magnetUri) {
    if (magnetUri == null) return null;
    try {
      // Parse raw string to avoid URI.create() failing on unencoded chars
      int queryStart = magnetUri.indexOf('?');
      if (queryStart < 0) return null;
      for (String param : magnetUri.substring(queryStart + 1).split("&")) {
        if (param.startsWith("xt=urn:btih:")) {
          String hash = param.substring("xt=urn:btih:".length());
          if (hash.length() == 40) return hash.toLowerCase();
          if (hash.length() == 32) return HexFormat.of().formatHex(new Base32().decode(hash.toUpperCase()));
        }
      }
    } catch (Exception e) {
      log.warn("Failed to extract info hash from magnet URI: {}", e.getMessage());
    }
    return null;
  }

  public static String extractFromTorrentUrl(String downloadUrl) {
    if (downloadUrl == null || downloadUrl.isBlank()) return null;
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(downloadUrl))
          .GET()
          .build();
      HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
      byte[] bytes = response.body();
      if (bytes == null || bytes.length == 0) return null;
      return extractFromTorrentBytes(bytes);
    } catch (Exception e) {
      log.warn("Failed to fetch/parse torrent from URL '{}': {}", downloadUrl, e.getMessage());
      return null;
    }
  }

  private static String extractFromTorrentBytes(byte[] data) throws Exception {
    if (data[0] != 'd') throw new IllegalArgumentException("Not a valid torrent file");
    int pos = 1;
    while (pos < data.length && data[pos] != 'e') {
      int[] keyResult = readString(data, pos);
      String key = new String(data, keyResult[0], keyResult[1] - keyResult[0]);
      int valStart = keyResult[1];
      if ("info".equals(key)) {
        int valEnd = skipValue(data, valStart);
        byte[] infoBytes = Arrays.copyOfRange(data, valStart, valEnd);
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        return HexFormat.of().formatHex(sha1.digest(infoBytes));
      }
      pos = skipValue(data, valStart);
    }
    throw new IllegalArgumentException("No info dictionary found in torrent file");
  }

  private static int skipValue(byte[] data, int pos) {
    byte b = data[pos];
    if (b == 'd' || b == 'l') {
      pos++;
      while (data[pos] != 'e') pos = skipValue(data, pos);
      return pos + 1;
    } else if (b == 'i') {
      return findByte(data, (byte) 'e', pos + 1) + 1;
    } else {
      int[] s = readString(data, pos);
      return s[1];
    }
  }

  private static int[] readString(byte[] data, int pos) {
    int sep = findByte(data, (byte) ':', pos);
    int len = Integer.parseInt(new String(data, pos, sep - pos));
    int start = sep + 1;
    return new int[]{start, start + len};
  }

  private static int findByte(byte[] data, byte target, int from) {
    for (int i = from; i < data.length; i++) {
      if (data[i] == target) return i;
    }
    throw new IllegalArgumentException("Unexpected end of torrent data");
  }
}
