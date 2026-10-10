package com.ruoyi.visit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CAS 单点登录配置（application.yml 里的 cas.* 段）。
 *
 * 默认 enabled=false，不影响现有账号密码登录。
 * 接学校统一身份认证（浙江农林大学 uis.zafu.edu.cn / portal.zfsoft.com）。
 */
@Component
@ConfigurationProperties(prefix = "cas")
public class CasProperties
{
    /** 是否启用 CAS 登录（默认 false，不影响现有登录） */
    private boolean enabled = false;

    /** CAS 服务端前缀，如 https://portal.zfsoft.com/cas */
    private String serverUrlPrefix = "https://portal.zfsoft.com/cas";

    /** CAS 登录地址，如 https://portal.zfsoft.com/cas/login */
    private String loginUrl = "https://portal.zfsoft.com/cas/login";

    /** 本应用 CAS 回调地址（必须与登录时的 service 完全一致） */
    private String callbackUrl = "";

    /** 校验失败/登录成功后重定向到前端页面的地址 */
    private String frontendUrl = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServerUrlPrefix() { return serverUrlPrefix; }
    public void setServerUrlPrefix(String serverUrlPrefix) { this.serverUrlPrefix = serverUrlPrefix; }
    public String getLoginUrl() { return loginUrl; }
    public void setLoginUrl(String loginUrl) { this.loginUrl = loginUrl; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }
}
