package com.ruoyi.visit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 接口限流（issue #32）：chat / voice 等调用 DeepSeek / 腾讯云 ASR 的接口。
 *
 * 按「用户 ID + IP」双维度限流，防止恶意循环调用耗尽 AI 额度。
 * 阈值可在 application.yml 的 visit.ai-rate-limit 段配置，enabled 预留开关。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AiRateLimit
{
    /** 限流时间窗口，单位秒（默认 60） */
    int time() default 60;

    /** 时间窗口内允许的最大调用次数（默认 30） */
    int count() default 30;
}
