package com.ruoyi.web.controller.visit;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.web.service.UserDetailsServiceImpl;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.visit.config.CasProperties;
import com.ruoyi.visit.service.ICasService;

/**
 * CAS 单点登录接入（浙江农林大学统一身份认证）。
 *
 * 流程：
 *   1. 前端访问 /cas/login → 重定向到 CAS 登录页（带 service=回调地址）
 *   2. CAS 认证成功 → 回调 /cas/callback?ticket=ST-xxx
 *   3. callback 校验 ticket → 拿 CODE（工号）→ 匹配 sys_user → 生成 token → 重定向前端
 *
 * 默认 cas.enabled=false，不影响现有账号密码登录。
 * 不自动建号：CODE 必须已在 sys_user.user_name 存在，否则拒绝。
 */
@RestController
@RequestMapping("/cas")
public class CasAuthController
{
    private static final Logger log = LoggerFactory.getLogger(CasAuthController.class);

    @Autowired
    private CasProperties casProperties;

    @Autowired
    private ICasService casService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenService tokenService;

    /**
     * 登录入口：重定向到 CAS 登录页
     */
    @GetMapping("/login")
    public void login(HttpServletResponse response) throws Exception
    {
        if (!casProperties.isEnabled())
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("CAS 登录未启用");
            return;
        }
        String callback = casProperties.getCallbackUrl();
        String url = casProperties.getLoginUrl() + "?service=" + URLEncoder.encode(callback, StandardCharsets.UTF_8);
        log.info("[CAS] 重定向到登录页：{}", url);
        response.sendRedirect(url);
    }

    /**
     * CAS 回调：校验 ticket，登录成功生成 token
     */
    @GetMapping("/callback")
    public void callback(@RequestParam(value = "ticket", required = false) String ticket,
                         HttpServletResponse response) throws Exception
    {
        String frontend = casProperties.getFrontendUrl();
        if (!casProperties.isEnabled())
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("CAS 登录未启用");
            return;
        }

        // 校验 ticket
        Map<String, String> attrs = casService.validateTicket(ticket, casProperties.getCallbackUrl());
        if (attrs == null)
        {
            log.error("[CAS] ticket 校验失败：{}", ticket);
            response.sendRedirect(frontend + "?casError=校验失败");
            return;
        }

        // 取工号（CODE）；若无则退回 user 字段
        String code = attrs.getOrDefault("CODE", attrs.getOrDefault("user", ""));
        if (StringUtils.isEmpty(code))
        {
            log.error("[CAS] 未获取到工号 CODE，属性：{}", attrs);
            response.sendRedirect(frontend + "?casError=未获取到工号");
            return;
        }

        // 匹配已存在账号（不自动建号）
        SysUser user = userService.selectUserByUserName(code);
        if (user == null)
        {
            log.warn("[CAS] 工号 {} 在系统中不存在，拒绝登录", code);
            response.sendRedirect(frontend + "?casError=账号未开通");
            return;
        }

        // 构建 LoginUser + 生成 token
        LoginUser loginUser = (LoginUser) userDetailsService.createLoginUser(user);
        String token = tokenService.createToken(loginUser);
        log.info("[CAS] 用户 {}（工号 {}）登录成功", user.getUserName(), code);

        // 重定向到前端，token 放 query（前端拿到后存 localStorage）
        response.sendRedirect(frontend + "?casToken=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
    }
}
