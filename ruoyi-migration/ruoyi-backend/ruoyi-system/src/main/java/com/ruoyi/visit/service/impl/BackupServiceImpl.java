package com.ruoyi.visit.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 数据库定时备份（issue #9）
 *
 * 策略：
 *   - 每天凌晨 2:00 自动备份；应用启动时若当天尚无备份则立即补一份
 *     （对齐旧版「启动时快照」行为，防止运行多日从未产生备份）
 *   - 每份快照一个目录：backups/backup-yyyy-MM-dd/
 *       ry-vue.sql      mysqldump 全库导出（结构+数据）
 *       photos.zip      图片附件目录打包（学生工作佐证材料）
 *       如何恢复.txt    恢复步骤说明
 *   - 滚动保留最近 keep 份（默认 14），超出自动清理
 *   - 备份失败仅记录日志，绝不影响业务运行
 *
 * 配置（application.yml visit.backup.*，密码走环境变量，不硬编码）：
 *   visit.backup.enabled   是否启用（默认 true）
 *   visit.backup.dir       快照存放根目录（默认 ./backups）
 *   visit.backup.keep      保留份数（默认 14）
 *   visit.backup.db-name   库名（默认 ry-vue）
 *   MYSQL_PASSWORD 环境变量供 mysqldump 使用（部署环境已注入）
 */
@Service
public class BackupServiceImpl
{
    private static final Logger log = LoggerFactory.getLogger(BackupServiceImpl.class);

    @Value("${visit.backup.enabled:true}")
    private boolean enabled;

    @Value("${visit.backup.dir:./backups}")
    private String backupDir;

    @Value("${visit.backup.keep:14}")
    private int keep;

    @Value("${visit.backup.db-name:ry-vue}")
    private String dbName;

    @Value("${visit.backup.db-user:root}")
    private String dbUser;

    @Value("${visit.backup.db-password:}")
    private String dbPassword;

    @Value("${visit.backup.db-host:127.0.0.1}")
    private String dbHost;

    /** 图片附件目录（与 ruoyi profile 配置保持一致） */
    @Value("${visit.backup.photos-dir:D:/ruoyi/uploadPath}")
    private String photosDir;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 每天凌晨 2:00 执行 */
    @Scheduled(cron = "${visit.backup.cron:0 0 2 * * ?}")
    public void scheduledBackup()
    {
        runBackup("定时");
    }

    /** 应用启动时：若当天尚无备份，立即补一份（旧版「启动快照」行为） */
    @Scheduled(initialDelay = 60_000, fixedDelay = Long.MAX_VALUE)
    public void startupBackup()
    {
        File today = new File(backupDir, "backup-" + LocalDate.now().format(DAY_FMT));
        if (!today.exists())
        {
            runBackup("启动");
        }
    }

    /** 执行一次备份，返回快照目录；失败返回 null（仅记日志，不抛出） */
    public File runBackup(String trigger)
    {
        if (!enabled)
        {
            return null;
        }
        String day = LocalDate.now().format(DAY_FMT);
        File snapDir = new File(backupDir, "backup-" + day);
        try
        {
            snapDir.mkdirs();

            // 1. mysqldump 导出数据库
            File sqlFile = new File(snapDir, dbName + ".sql");
            dumpDatabase(sqlFile);

            // 2. 打包图片附件目录
            zipPhotos(new File(snapDir, "photos.zip"));

            // 3. 写恢复说明
            writeRecoveryGuide(new File(snapDir, "如何恢复.txt"), day);

            log.info("[backup] {}备份成功: {}", trigger, snapDir.getAbsolutePath());

            // 4. 滚动清理旧快照
            cleanOldSnapshots();
            return snapDir;
        }
        catch (Exception e)
        {
            log.error("[backup] {}备份失败: {}", trigger, e.getMessage(), e);
            return null;
        }
    }

    /** mysqldump 导出全库（结构+数据+存储过程） */
    private void dumpDatabase(File out) throws IOException, InterruptedException
    {
        List<String> cmd = new ArrayList<>();
        cmd.add("mysqldump");
        cmd.add("-h" + dbHost);
        cmd.add("-u" + dbUser);
        if (dbPassword != null && !dbPassword.isEmpty())
        {
            cmd.add("-p" + dbPassword);
        }
        cmd.add("--single-transaction");           // InnoDB 一致性快照，不锁表
        cmd.add("--routines");
        cmd.add("--default-character-set=utf8mb4");
        cmd.add(dbName);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);
        Process proc = pb.start();

        // stdout 写入 sql 文件；stderr 单独读取（密码告警在 stderr，属正常）
        try (var in = proc.getInputStream(); var fout = Files.newOutputStream(out.toPath()))
        {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0)
            {
                fout.write(buf, 0, n);
            }
        }
        StringBuilder err = new StringBuilder();
        try (var errStream = proc.getErrorStream())
        {
            byte[] buf = new byte[1024];
            int n;
            while ((n = errStream.read(buf)) > 0)
            {
                err.append(new String(buf, 0, n));
            }
        }
        int code = proc.waitFor();
        if (code != 0)
        {
            throw new IOException("mysqldump 退出码 " + code + ": " + err);
        }
        if (out.length() < 1024)
        {
            throw new IOException("mysqldump 输出过小，疑似失败: " + err);
        }
    }

    /** 图片附件目录打 zip */
    private void zipPhotos(File out) throws IOException
    {
        Path src = Paths.get(photosDir);
        if (!Files.exists(src))
        {
            // 目录不存在时写一个空标记，避免恢复时误判
            try (var zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(out.toPath())))
            {
                zos.close();
            }
            return;
        }
        try (var zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(out.toPath()));
             Stream<Path> walk = Files.walk(src))
        {
            walk.filter(Files::isRegularFile).forEach(f ->
            {
                try
                {
                    String entryName = src.relativize(f).toString().replace('\\', '/');
                    zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                    Files.copy(f, zos);
                    zos.closeEntry();
                }
                catch (IOException ignored) { }
            });
        }
    }

    /** 恢复说明（快照自带，防恢复方法失传） */
    private void writeRecoveryGuide(File f, String day) throws IOException
    {
        String text = "备份快照 " + day + "\n"
            + "====================\n"
            + "内容：\n"
            + "  " + dbName + ".sql   数据库全量导出（结构+数据）\n"
            + "  photos.zip       图片附件（对应 ruoyi profile 上传目录）\n"
            + "\n"
            + "恢复步骤：\n"
            + "  1. 恢复数据库：\n"
            + "     mysql -u root -p " + dbName + " < " + dbName + ".sql\n"
            + "  2. 恢复图片：将 photos.zip 解压到 ruoyi profile 目录（如 D:/ruoyi/uploadPath）\n"
            + "  3. 重启后端服务\n";
        Files.write(f.toPath(), text.getBytes("UTF-8"));
    }

    /** 只保留最近 keep 份，超出删除最旧的 */
    private void cleanOldSnapshots()
    {
        File root = new File(backupDir);
        File[] dirs = root.listFiles(f -> f.isDirectory() && f.getName().startsWith("backup-"));
        if (dirs == null || dirs.length <= keep)
        {
            return;
        }
        Arrays.sort(dirs, Comparator.comparing(File::getName).reversed());
        for (int i = keep; i < dirs.length; i++)
        {
            deleteDir(dirs[i]);
            log.info("[backup] 清理过期快照: {}", dirs[i].getName());
        }
    }

    private void deleteDir(File dir)
    {
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (File f : files)
            {
                if (f.isDirectory()) deleteDir(f); else f.delete();
            }
        }
        dir.delete();
    }
}
