package com.ruoyi.visit.service.impl;

import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.visit.config.VisitConfig;
import com.ruoyi.visit.service.IAsrService;
import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;

/**
 * 腾讯云一句话识别（SentenceRecognition）。迁移自旧系统 AsrServiceImpl。
 */
@Service
public class AsrServiceImpl implements IAsrService
{
    private static final Logger log = LoggerFactory.getLogger(AsrServiceImpl.class);

    private final VisitConfig config;

    public AsrServiceImpl(VisitConfig config)
    {
        this.config = config;
    }

    @Override
    public String transcribeWav(byte[] data)
    {
        String secretId = config.getTencent().getSecretId();
        String secretKey = config.getTencent().getSecretKey();
        String appId = config.getTencent().getAsrAppid();

        if (isBlank(secretId) || isBlank(secretKey) || isBlank(appId))
        {
            log.warn("腾讯云语音识别调用失败：缺少 secret-id / secret-key / asr-appid 之一");
            throw new ServiceException("未配置语音识别凭证，请先在配置里填写腾讯云三项配置");
        }

        try
        {
            Credential cred = new Credential(secretId, secretKey);
            AsrClient client = new AsrClient(cred, "");

            SentenceRecognitionRequest req = new SentenceRecognitionRequest();
            req.setProjectId(0L);
            req.setSubServiceType(2L);
            req.setEngSerViceType("16k_zh");
            req.setSourceType(1L);
            req.setVoiceFormat("wav");
            req.setData(Base64.getEncoder().encodeToString(data));
            req.setDataLen((long) data.length);

            SentenceRecognitionResponse resp = client.SentenceRecognition(req);
            String text = resp.getResult() == null ? "" : resp.getResult().strip();
            if (text.isEmpty())
            {
                log.info("腾讯云 ASR 返回成功但结果为空（AudioDuration={}）", resp.getAudioDuration());
            }
            return text;
        }
        catch (TencentCloudSDKException e)
        {
            throw new ServiceException(friendlyError(e.getErrorCode(), e.getMessage()));
        }
        catch (Exception e)
        {
            log.warn("腾讯云 ASR 调用失败：{}", e.getMessage());
            throw new ServiceException("语音识别失败，请稍后重试");
        }
    }

    private String friendlyError(String code, String msg)
    {
        if (code == null)
        {
            code = "";
        }
        if ("AuthFailure.UnauthorizedOperation".equals(code))
        {
            return "语音识别权限未生效：请确认已在腾讯云控制台开通「语音识别 ASR」服务，且密钥状态为启用";
        }
        if (code.contains("AuthFailure"))
        {
            return "腾讯云密钥无效，请检查 SecretId/SecretKey 是否填对";
        }
        if (code.contains("LimitExceeded"))
        {
            return "语音识别免费额度已用完或接口调用过频，请稍后再试";
        }
        if (code.contains("ErrorRecognize"))
        {
            return "没听清，录音里没有识别出人声，请再试一次";
        }
        if (code.contains("InvalidParameter"))
        {
            return "录音格式不支持，请重试（建议时长 1~60 秒）";
        }
        if (!code.isEmpty())
        {
            return "语音识别失败：" + code;
        }
        return (msg == null || msg.isEmpty()) ? "语音识别失败，请稍后重试" : "语音识别失败：" + truncate(msg, 150);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private String truncate(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }
}
