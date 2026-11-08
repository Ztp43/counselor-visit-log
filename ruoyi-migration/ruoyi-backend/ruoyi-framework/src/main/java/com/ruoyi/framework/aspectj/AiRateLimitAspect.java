package com.ruoyi.framework.aspectj;

import java.util.Collections;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.visit.annotation.AiRateLimit;
import com.ruoyi.visit.config.VisitConfig;

/**
 * AI 接口限流切面（issue #32）。
 *
 * 复用若依 RedisConfig.limitScript（原子计数 Lua），key 拼接「用户 ID + IP」，
 * 实现按用户 + 客户端双维度限流，防止恶意循环调用耗尽 DeepSeek / 腾讯云 ASR 额度。
 * 放在 framework 模块（有 aspectj 依赖，且依赖 system 可取 VisitConfig）。
 */
@Aspect
@Component
public class AiRateLimitAspect
{
    private final VisitConfig config;
    private RedisTemplate<Object, Object> redisTemplate;
    private DefaultRedisScript<Long> limitScript;

    public AiRateLimitAspect(VisitConfig config)
    {
        this.config = config;
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate<Object, Object> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public void setLimitScript(DefaultRedisScript<Long> limitScript)
    {
        this.limitScript = limitScript;
    }

    @Before("@annotation(aiRateLimit)")
    public void doBefore(JoinPoint point, AiRateLimit aiRateLimit) throws Throwable
    {
        // 开关关闭则不限流
        if (!config.getAiRateLimit().isEnabled())
        {
            return;
        }

        // 注解值优先，未显式设置时回退到 yml 配置
        int time = aiRateLimit.time() > 0 ? aiRateLimit.time() : config.getAiRateLimit().getTime();
        int count = aiRateLimit.count() > 0 ? aiRateLimit.count() : config.getAiRateLimit().getCount();

        String key = buildKey();
        try
        {
            List<Object> keys = Collections.singletonList(key);
            Long number = redisTemplate.execute(limitScript, keys, count, time);
            if (StringUtils.isNull(number) || number.intValue() > count)
            {
                throw new ServiceException("调用过于频繁，请稍候再试（" + time + " 秒内最多 " + count + " 次）");
            }
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            // Redis 异常时放行（限流故障不影响正常业务）
        }
    }

    /** key = 限流前缀 + userId + IP */
    private String buildKey()
    {
        Long userId = SecurityUtils.getUserId();
        String ip = IpUtils.getIpAddr();
        return "visit:ai:rate:" + (userId != null ? userId : "anon") + ":" + (ip != null ? ip : "unknown");
    }
}
