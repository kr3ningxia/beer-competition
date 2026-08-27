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
     * 按当前登录身份校验并读取文件。调用方不能传入存储路径绕过资产授权。
     */
    com.beercompetition.pojo.vo.FileDownloadVO download(Long fileAssetId);

    /**
     * 读取允许公开展示的文件。该方法只接受公共文件白名单内的资产。
     */
    com.beercompetition.pojo.vo.FileDownloadVO downloadPublic(Long fileAssetId);

    /**
     * 生成受控公共文件地址；敏感文件返回 {@code null}。
     */
    String publicUrl(Long fileAssetId);

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
