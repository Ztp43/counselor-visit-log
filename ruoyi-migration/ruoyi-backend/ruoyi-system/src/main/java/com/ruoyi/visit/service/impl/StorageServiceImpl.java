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
 * issue #10 目录统一：图片与录音都归入 profile 目录树——
 *   图片  {profile}/xxx.jpg        URL 走 /profile/xxx.jpg
 *   录音  {profile}/voice/xxx.wav  仅存档，无需 URL 访问
 * 兼容回退：目录统一前录入的录音在旧目录（visit.upload.legacy-dir，即 ./uploads），
 *   读取/删除时若新目录没有该文件，回退旧目录处理，历史记录不失效。
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
        // 新旧目录都尝试删（旧目录残留文件也能清掉）
        delete(config.getUpload().getDir(), filename);
        delete(config.getUpload().getLegacyDir(), filename);
    }

    /** 录音文件解析：新目录优先，找不到回退旧目录（历史数据兼容） */
    private Path resolveAudio(String filename)
    {
        Path newPath = Paths.get(config.getUpload().getDir()).toAbsolutePath().resolve(filename);
        if (Files.exists(newPath))
        {
            return newPath;
        }
        Path legacyPath = Paths.get(config.getUpload().getLegacyDir()).toAbsolutePath().resolve(filename);
        if (Files.exists(legacyPath))
        {
            return legacyPath;
        }
        return newPath; // 都不存在时返回新路径（由调用方处理不存在的情况）
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
