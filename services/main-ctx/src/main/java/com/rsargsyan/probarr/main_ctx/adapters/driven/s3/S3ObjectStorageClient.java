package com.rsargsyan.probarr.main_ctx.adapters.driven.s3;

import com.rsargsyan.probarr.main_ctx.core.ports.client.ObjectStorageClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Slf4j
@Component
public class S3ObjectStorageClient implements ObjectStorageClient {

  @Value("${probarr.s3.access-key-id}")
  private String accessKeyId;

  @Value("${probarr.s3.secret-access-key}")
  private String secretAccessKey;

  @Value("${probarr.s3.region}")
  private String region;

  @Value("${probarr.s3.endpoint}")
  private String endpoint;

  @Value("${probarr.s3.bucket}")
  private String bucket;

  private S3Client s3Client;

  @PostConstruct
  public void init() {
    var credentials = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    URI endpointUri = URI.create(endpoint.contains("://") ? endpoint : "https://" + endpoint);
    var s3Config = S3Configuration.builder().pathStyleAccessEnabled(true).build();
    s3Client = S3Client.builder()
        .credentialsProvider(credentials)
        .region(Region.of(region))
        .endpointOverride(endpointUri)
        .serviceConfiguration(s3Config)
        .build();
  }

  @Override
  public void upload(String key, byte[] bytes, String contentType) {
    s3Client.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
        RequestBody.fromBytes(bytes)
    );
  }

  @Override
  public byte[] download(String key) {
    return s3Client.getObjectAsBytes(
        GetObjectRequest.builder().bucket(bucket).key(key).build()
    ).asByteArray();
  }
}
