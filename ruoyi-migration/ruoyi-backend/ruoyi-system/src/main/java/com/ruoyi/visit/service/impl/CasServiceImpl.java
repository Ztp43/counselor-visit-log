package com.ruoyi.visit.service.impl;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.visit.config.CasProperties;
import com.ruoyi.visit.service.ICasService;

/**
 * CAS 单点登录服务：校验 ticket、解析用户属性。
 * 标准 CAS 2.0 serviceValidate，返回 XML，用 JDK 自带 DOM 解析（不引第三方）。
 */
@Service
public class CasServiceImpl implements ICasService
{
    private static final Logger log = LoggerFactory.getLogger(CasServiceImpl.class);

    private final CasProperties casProperties;

    public CasServiceImpl(CasProperties casProperties)
    {
        this.casProperties = casProperties;
    }

    @Override
    public Map<String, String> validateTicket(String ticket, String service)
    {
        if (ticket == null || ticket.isBlank())
        {
            return null;
        }
        // serviceValidate?ticket=xxx&service=yyy（service 需 URL 编码）
        String url = casProperties.getServerUrlPrefix() + "/serviceValidate"
                + "?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8)
                + "&service=" + URLEncoder.encode(service, StandardCharsets.UTF_8);

        log.info("[CAS] 校验 ticket 请求：{}", url);
        String xml;
        try
        {
            xml = HttpUtils.sendGet(url);
        }
        catch (Exception e)
        {
            log.error("[CAS] 校验 ticket 请求失败：{}", e.getMessage());
            throw new RuntimeException("CAS 校验请求失败", e);
        }

        if (xml == null || xml.isBlank())
        {
            log.error("[CAS] 校验返回空响应");
            return null;
        }
        return parseCasXml(xml);
    }

    /**
     * 解析 serviceValidate 返回的 XML：
     * <cas:serviceResponse>
     *   <cas:authenticationSuccess>
     *     <cas:user>xxx</cas:user>
     *     <cas:attributes>
     *       <cas:CODE>工号</cas:CODE>
     *       <cas:XM>姓名</cas:XM>
     *       ...
     *     </cas:attributes>
     *   </cas:authenticationSuccess>
     *   （失败则为 <cas:authenticationFailure>）
     * </cas:serviceResponse>
     */
    private Map<String, String> parseCasXml(String xml)
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 防止 XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // 校验失败
            NodeList failures = doc.getElementsByTagNameNS("*", "authenticationFailure");
            if (failures.getLength() > 0)
            {
                Element f = (Element) failures.item(0);
                String code = f.getAttribute("code");
                log.error("[CAS] 校验失败，code={}", code);
                return null;
            }

            // 成功：提取 user + attributes
            Map<String, String> attrs = new LinkedHashMap<>();
            NodeList userNodes = doc.getElementsByTagNameNS("*", "user");
            if (userNodes.getLength() > 0)
            {
                attrs.put("user", userNodes.item(0).getTextContent().trim());
            }

            // attributes 下的所有子节点（CODE/XM/YHLX/DWH/DWMC/TENANTID/SIGNDATAID/MSSPID）
            NodeList attrNodes = doc.getElementsByTagNameNS("*", "attributes");
            if (attrNodes.getLength() > 0)
            {
                Element attrsEl = (Element) attrNodes.item(0);
                NodeList children = attrsEl.getChildNodes();
                for (int i = 0; i < children.getLength(); i++)
                {
                    if (children.item(i) instanceof Element)
                    {
                        Element el = (Element) children.item(i);
                        String key = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
                        attrs.put(key, el.getTextContent().trim());
                    }
                }
            }
            return attrs;
        }
        catch (Exception e)
        {
            log.error("[CAS] 解析响应 XML 失败：{}", e.getMessage());
            return null;
        }
    }
}
