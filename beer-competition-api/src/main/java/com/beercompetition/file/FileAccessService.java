package com.beercompetition.file;

/**
 * 根据文件元数据及其关联业务资源校验读取和管理权限。
 *
 * <p>文件下载必须先解析关联比赛或主办方，再执行组织范围校验；底层
 * {@code FileStorageService} 只负责字节存取，不承担授权。文件归属字段落地前
 * 不提供默认实现。</p>
 */
public interface FileAccessService {

    /**
     * 要求当前身份可以下载指定文件。
     *
     * @param fileAssetId 文件资产 ID
     */
    void requireDownloadAccess(Long fileAssetId);

    /**
     * 要求当前后台账号可以替换或删除指定文件。
     *
     * @param fileAssetId 文件资产 ID
     */
    void requireManagementAccess(Long fileAssetId);
}
