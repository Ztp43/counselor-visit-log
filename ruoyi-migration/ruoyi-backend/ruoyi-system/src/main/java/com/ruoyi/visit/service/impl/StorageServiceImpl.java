package com.ruoyi.visit.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.visit.config.VisitConfig;
import com.ruoyi.visit.service.IStorageService;

/**
 * 文件存储（图片/录音），纯 Java NIO。迁移自旧系统 StorageServiceImpl。
 * 图片存到若依 profile 目录（RuoYiConfig.getProfile()），URL 走 /profile/xxx.jpg；
 * 录音存到 visit.upload.dir（无需 URL 访问）。
 */
@Service
public class StorageServiceImpl implements IStorageService
{
    private static final Set<String> ALLOWED_EXT =
            Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");

    private final VisitConfig config;

    public StorageServiceImpl(VisitConfig config)
    {
        this.config = config;
    }

    @Override
    public String saveImage(byte[] data, String originalFilename)
    {
        String ext = ".jpg";
        if (originalFilename != null && originalFilename.contains("."))
        {
            String e = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ALLOWED_EXT.contains(e))
            {
                ext = e;
            }
        }
        // 图片存 profile 目录，返回文件名（前端拼 /profile/文件名 访问）
        return write(RuoYiConfig.getProfile(), data, ext);
    }

    @Override
    public String saveAudio(byte[] data)
    {
        return write(config.getUpload().getDir(), data, ".wav");
    }

    @Override
    public void deleteImage(String filename)
    {
        delete(RuoYiConfig.getProfile(), filename);
    }

    @Override
    public void deleteAudio(String filename)
    {
        delete(config.getUpload().getDir(), filename);
    }

    private void delete(String dirStr, String filename)
    {
        if (filename == null || filename.isBlank())
        {
            return;
        }
        try
        {
            Path dir = Paths.get(dirStr).toAbsolutePath();
            Files.deleteIfExists(dir.resolve(filename));
        }
        catch (IOException ignored)
        {
            // 删除失败不影响主流程（文件残留可后续清理）
        }
    }

    private String write(String dirStr, byte[] data, String ext)
    {
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(dirStr).toAbsolutePath();
        try
        {
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), data);
        }
        catch (IOException e)
        {
            throw new RuntimeException("文件保存失败", e);
        }
        return name;
    }
}
