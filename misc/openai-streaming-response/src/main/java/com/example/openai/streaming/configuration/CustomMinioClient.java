package com.example.openai.streaming.configuration;

import com.google.common.collect.Multimap;
import io.minio.CreateMultipartUploadResponse;
import io.minio.ListPartsResponse;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.errors.*;
import io.minio.messages.Part;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class CustomMinioClient extends MinioClient {

    /**
     * 继承父类
     * @param client
     */
    public CustomMinioClient(MinioClient client) {
        super(client);
    }


    /**
     * 初始化分片上传、获取 uploadId
     *
     * @param bucket           String  存储桶名称
     * @param region           String
     * @param object           String   文件名称
     * @param headers          Multimap<String, String> 请求头
     * @param extraQueryParams Multimap<String, String>
     * @return String
     */
    public String initMultiPartUpload(String bucket, String region, String object, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException {
        CreateMultipartUploadResponse response = this.createMultipartUpload(bucket, region, object, headers, extraQueryParams);
        return response.result().uploadId();
    }

    /**
     * 合并分片
     *
     * @param bucketName       String   桶名称
     * @param region           String
     * @param objectName       String   文件名称
     * @param uploadId         String   上传的 uploadId
     * @param parts            Part[]   分片集合
     * @param extraHeaders     Multimap<String, String>
     * @param extraQueryParams Multimap<String, String>
     * @return ObjectWriteResponse
     */
    public ObjectWriteResponse mergeMultipartUpload(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws IOException, NoSuchAlgorithmException, InsufficientDataException, ServerException, InternalException, XmlParserException, InvalidResponseException, ErrorResponseException, ServerException, InvalidKeyException {
        return this.completeMultipartUpload(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams);
    }

    /**
     * 查询当前上传后的分片信息
     *
     * @param bucketName       String   桶名称
     * @param region           String
     * @param objectName       String   文件名称
     * @param maxParts         Integer  分片数量
     * @param partNumberMarker Integer  分片起始值
     * @param uploadId         String   上传的 uploadId
     * @param extraHeaders     Multimap<String, String>
     * @param extraQueryParams Multimap<String, String>
     * @return ListPartsResponse
     */
    public ListPartsResponse listMultipart(String bucketName, String region, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, ServerException, XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        return this.listParts(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams);
    }


}

