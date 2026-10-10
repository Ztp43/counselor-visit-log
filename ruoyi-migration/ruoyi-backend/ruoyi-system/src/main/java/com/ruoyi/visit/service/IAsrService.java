package com.ruoyi.visit.service;

/**
 * 语音转写（腾讯云一句话识别）。迁移自旧系统 AsrService。
 */
public interface IAsrService
{
    /** 把 wav 音频字节转成文字 */
    String transcribeWav(byte[] data);
}
