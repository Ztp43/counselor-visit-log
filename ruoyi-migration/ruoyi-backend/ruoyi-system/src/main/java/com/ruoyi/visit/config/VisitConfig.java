package com.ruoyi.visit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 走访模块自定义配置（application.yml 里的 visit.* 段）。
 * 迁移自旧系统 AppProperties，AI/语音/文件存储相关。
 */
@Component
@ConfigurationProperties(prefix = "visit")
public class VisitConfig
{
    private Deepseek deepseek = new Deepseek();
    private Tencent tencent = new Tencent();
    private Upload upload = new Upload();

    public Deepseek getDeepseek() { return deepseek; }
    public void setDeepseek(Deepseek deepseek) { this.deepseek = deepseek; }
    public Tencent getTencent() { return tencent; }
    public void setTencent(Tencent tencent) { this.tencent = tencent; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }

    public static class Deepseek
    {
        private String apiKey = "";
        private String baseUrl = "https://api.deepseek.com";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Tencent
    {
        private String secretId = "";
        private String secretKey = "";
        private String asrAppid = "";
        public String getSecretId() { return secretId; }
        public void setSecretId(String secretId) { this.secretId = secretId; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getAsrAppid() { return asrAppid; }
        public void setAsrAppid(String asrAppid) { this.asrAppid = asrAppid; }
    }

    public static class Upload
    {
        private String dir = "./uploads";
        /** 旧录音目录（issue #10 目录统一前的位置，兼容回退用） */
        private String legacyDir = "./uploads";
        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public String getLegacyDir() { return legacyDir; }
        public void setLegacyDir(String legacyDir) { this.legacyDir = legacyDir; }
    }
}
