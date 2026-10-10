package com.ruoyi.visit.service;

/**
 * 文件存储（图片/录音）。迁移自旧系统 StorageService。
 */
public interface IStorageService
{
    String saveImage(byte[] data, String originalFilename);

    String saveAudio(byte[] data);

    /** 删除图片文件（按文件名，位于 profile 目录；不存在则忽略） */
    void deleteImage(String filename);

    /** 删除录音文件（按文件名，位于 upload 目录；不存在则忽略） */
    void deleteAudio(String filename);
}
