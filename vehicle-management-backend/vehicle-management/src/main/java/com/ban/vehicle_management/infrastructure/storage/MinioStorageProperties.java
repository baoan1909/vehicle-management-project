package com.ban.vehicle_management.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage.minio")
public class MinioStorageProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String publicBucket;
    private String privateBucket;
    private int presignedUrlExpireSeconds = 900;
    private String publicUrlBase;
    private long imageMaxSizeBytes = 5 * 1024 * 1024;
    private int imageMaxWidthPixels = 1280;
    private double imageJpegQuality = 0.85;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublicBucket() {
        return publicBucket;
    }

    public void setPublicBucket(String publicBucket) {
        this.publicBucket = publicBucket;
    }

    public String getPrivateBucket() {
        return privateBucket;
    }

    public void setPrivateBucket(String privateBucket) {
        this.privateBucket = privateBucket;
    }

    public int getPresignedUrlExpireSeconds() {
        return presignedUrlExpireSeconds;
    }

    public void setPresignedUrlExpireSeconds(int presignedUrlExpireSeconds) {
        this.presignedUrlExpireSeconds = presignedUrlExpireSeconds;
    }

    public String getPublicUrlBase() {
        return publicUrlBase;
    }

    public void setPublicUrlBase(String publicUrlBase) {
        this.publicUrlBase = publicUrlBase;
    }

    public long getImageMaxSizeBytes() {
        return imageMaxSizeBytes;
    }

    public void setImageMaxSizeBytes(long imageMaxSizeBytes) {
        this.imageMaxSizeBytes = imageMaxSizeBytes;
    }

    public int getImageMaxWidthPixels() {
        return imageMaxWidthPixels;
    }

    public void setImageMaxWidthPixels(int imageMaxWidthPixels) {
        this.imageMaxWidthPixels = imageMaxWidthPixels;
    }

    public double getImageJpegQuality() {
        return imageJpegQuality;
    }

    public void setImageJpegQuality(double imageJpegQuality) {
        this.imageJpegQuality = imageJpegQuality;
    }
}
