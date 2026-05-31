package com.beercompetition.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class AliOssUtil {

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucketName;

    public AliOssUtil(String endpoint, String accessKeyId, String accessKeySecret, String bucketName) {
        this.endpoint = endpoint;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.bucketName = bucketName;
    }

    public String upload(String objectName, byte[] bytes) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.putObject(bucketName, objectName, new java.io.ByteArrayInputStream(bytes));
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } finally {
            ossClient.shutdown();
        }
    }
}
