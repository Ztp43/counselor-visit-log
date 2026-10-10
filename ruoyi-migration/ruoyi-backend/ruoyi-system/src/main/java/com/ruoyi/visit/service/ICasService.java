package com.ruoyi.visit.service;

import java.util.Map;

/**
 * CAS 单点登录服务：校验 ticket、解析用户属性。
 */
public interface ICasService
{
    /**
     * 校验 CAS ticket，返回用户属性（CODE=工号、XM=姓名 等）。
     *
     * @param ticket   CAS 服务器回调时带的 ticket（ST-xxx）
     * @param service  回调地址（必须与登录时传给 CAS 的 service 完全一致）
     * @return 属性 Map；校验失败返回 null 或抛异常
     */
    Map<String, String> validateTicket(String ticket, String service);
}
