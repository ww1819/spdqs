package com.qs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    private String dir = "./data/uploads";
    private long maxImageSize = 10 * 1024 * 1024L;
    private long maxFileSize = 50 * 1024 * 1024L;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public long getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(long maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
