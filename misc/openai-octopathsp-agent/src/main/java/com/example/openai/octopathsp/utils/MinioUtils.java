package com.example.openai.octopathsp.utils;


import cn.hutool.core.collection.CollectionUtil;
import com.example.openai.octopathsp.configuration.CustomMinioClient;
import io.micrometer.common.util.StringUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import io.minio.messages.Part;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
@Component
public class MinioUtils {

    @Value(value = "${minio.endpoint}")
    private String endpoint;
    @Value(value = "${minio.accesskey}")
    private String accesskey;
    @Value(value = "${minio.secretkey}")
    private String secretkey;

    @Value(value = "${minio.expiry}")
    private Integer expiry;

    private CustomMinioClient customMinioClient;

    private MinioClient minioClientx;

    /**
     * 用spring的自动注入会注入失败
     */
    @PostConstruct
    public void init() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesskey, secretkey)
                .build();
        minioClientx = minioClient;
        customMinioClient = new CustomMinioClient(minioClient);
    }


    /**
     * 单文件签名上传
     *
     * @param objectName 文件全路径名称
     * @param bucketName 桶名称
     * @return /
     */
    public Map<String, Object> getUploadObjectUrl(String objectName, String bucketName) {
        try {
            log.info("tip message: 通过 <{}-{}> 开始单文件上传<minio>", objectName, bucketName);
            Map<String, Object> resMap = new HashMap();
            List<String> partList = new ArrayList<>();
            String url = customMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiry, TimeUnit.DAYS)
                            .build());
            log.info("tip message: 单个文件上传、成功");
            partList.add(url);
            resMap.put("uploadId", "SingleFileUpload");
            resMap.put("urlList", partList);
            return resMap;
        } catch (Exception e) {
            log.error("error message: 单个文件上传失败、原因:", e);
            // 返回 文件上传失败
            return null;
        }
    }

    /**
     * 分片上传完后合并
     *
     * @param objectName 文件全路径名称
     * @param uploadId   返回的uploadId
     * @param bucketName 桶名称
     * @return boolean
     */
    public boolean mergeMultipartUpload(String objectName, String uploadId, String bucketName) {
        try {
            log.info("tip message: 通过 <{}-{}-{}> 合并<分片上传>数据", objectName, uploadId, bucketName);
            //目前仅做了最大1000分片
            Part[] parts = new Part[1000];
            // 查询上传后的分片数据
            ListPartsResponse partResult = customMinioClient.listMultipart(bucketName, null, objectName, 1000, 0, uploadId, null, null);
            int partNumber = 1;
            for (Part part : partResult.result().partList()) {
                parts[partNumber - 1] = new Part(partNumber, part.etag());
                partNumber++;
            }
            // 合并分片
            customMinioClient.mergeMultipartUpload(bucketName, null, objectName, uploadId, parts, null, null);

        } catch (Exception e) {
            log.error("error message: 合并失败、原因:", e);
            //TODO删除redis的数据
            return false;
        }
        return true;
    }

    /**
     * 通过 sha256 获取上传中的分片信息
     *
     * @param objectName 文件全路径名称
     * @param uploadId   返回的uploadId
     * @param bucketName 桶名称
     * @return Mono<Map < String, Object>>
     */
    public List<Integer> getChunkByFileMD5(String objectName, String uploadId, String bucketName) {
        log.info("通过 <{}-{}-{}> 查询<minio>上传分片数据", objectName, uploadId, bucketName);
        try {
            // 查询上传后的分片数据
            ListPartsResponse partResult = customMinioClient.listMultipart(bucketName, null, objectName, 1000, 0, uploadId, null, null);
            return partResult.result().partList().stream().map(Part::partNumber).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("error message: 查询上传后的分片信息失败、原因:", e);
            return null;
        }
    }


    /**
     * 创建一个桶
     *
     * @return
     */
    public String createBucket(String bucketName) {
        try {
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build();

            // 检查桶是否存在
            if (customMinioClient.bucketExists(bucketExistsArgs)) {
                return bucketName + " (已存在)";
            }

            // 创建新桶
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build();
            customMinioClient.makeBucket(makeBucketArgs);

            // 设置桶策略为public (允许匿名读取)
            String policyJson = String.format(
                    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\"],\"Resource\":[\"arn:aws:s3:::%s\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}",
                    bucketName, bucketName
            );

            SetBucketPolicyArgs setPolicyArgs = SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policyJson)
                    .build();

            customMinioClient.setBucketPolicy(setPolicyArgs);

            return bucketName + " (创建成功并设置为public)";
        } catch (MinioException e) {
            log.error("MinIO错误: {}", e.getMessage());
            throw new RuntimeException("MinIO操作失败", e);
        } catch (Exception e) {
            log.error("创建桶失败：{}", e.getMessage());
            throw new RuntimeException("系统错误", e);
        }
    }


    /**
     * 根据文件类型获取minio桶名称
     *
     * @param fileType
     * @return
     */
    public String getBucketName(String fileType) {
        try {
            //String bucketName = getProperty(fileType.toLowerCase());

            if (fileType != null && !fileType.equals("")) {
                //判断桶是否存在
                String bucketName2 = createBucket(fileType.toLowerCase());
                if (bucketName2 != null && !bucketName2.equals("")) {
                    return bucketName2;
                } else {
                    return fileType;
                }
            }

        } catch (Exception e) {

            log.error("Error reading bucket name ");
        }
        return fileType;
    }

    /**
     * 读取配置文件
     *
     * @param fileType
     * @return
     * @throws IOException
     */
    private String getProperty(String fileType) throws IOException {
        Properties SysLocalPropObject = new Properties();
        //判断桶关系配置文件是否为空
        if (SysLocalPropObject.isEmpty()) {
            InputStream is = getClass().getResourceAsStream("/BucketRelation.properties");
            SysLocalPropObject.load(is);
            is.close();
        }
        return SysLocalPropObject.getProperty("bucket." + fileType);
    }

    /**
     * 文件上传
     *
     * @param file 文件
     * @return Boolean
     */
    public String upload(MultipartFile file, String bucketName) {
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException();
        }
        String objectName = file.getOriginalFilename();
        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(bucketName).object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build();
            //文件名称相同会覆盖
            customMinioClient.putObject(objectArgs);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            e.printStackTrace();
            return null;
        }
        // 查看文件地址
        GetPresignedObjectUrlArgs build = new GetPresignedObjectUrlArgs().builder().bucket(bucketName).object(objectName).method(Method.GET).build();
        String url = null;
        try {
            url = customMinioClient.getPresignedObjectUrl(build);
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * 文件上传
     *
     * @param file 文件
     * @return Boolean
     */
    public String uploadAlias(MultipartFile file, String bucketName, String fileName) {
        try {
            PutObjectArgs objectArgs = PutObjectArgs.builder().bucket(bucketName).object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build();
            //文件名称相同会覆盖
            customMinioClient.putObject(objectArgs);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            e.printStackTrace();
            return null;
        }
        // 查看文件地址
        GetPresignedObjectUrlArgs build = new GetPresignedObjectUrlArgs().builder().bucket(bucketName).object(fileName).method(Method.GET).build();
        String url = null;
        try {
            url = customMinioClient.getPresignedObjectUrl(build);
        } catch (ErrorResponseException e) {
            e.printStackTrace();
        } catch (InsufficientDataException e) {
            e.printStackTrace();
        } catch (InternalException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (XmlParserException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }

        return url;
    }

    /**
     * 创建一个桶
     *
     * @return
     */
    public String deleteBucket(String bucketName) {
        try {
            if (StringUtils.isBlank(bucketName)) {
                return bucketName;
            }
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
            if (!customMinioClient.bucketExists(bucketExistsArgs)) {
                log.warn("桶{}不存在", bucketName);
                return bucketName;
            }
            List<String> items = new ArrayList<>();
            Iterable<Result<Item>> objects = customMinioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
            for (Result<Item> result : objects) {
                Item item = result.get();
                items.add(item.objectName());
            }
            if (CollectionUtil.isNotEmpty(items)) {
                items.forEach(e -> removeFile(bucketName, e));
            }

            customMinioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            return bucketName;
        } catch (Exception e) {
            log.error("创建桶失败：{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public int removeFile(String bucketName, String fileName) {
        try {
            //判断桶是否存在
            boolean res = customMinioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (res) {
                //删除文件
                customMinioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName)
                        .object(fileName).build());
            }
        } catch (Exception e) {
            System.out.println("删除文件失败");
            e.printStackTrace();
            return 1;
        }
        System.out.println("删除文件成功");
        return 0;
    }


    /**
     * 下载一个文件
     */
    public InputStream download(String bucket, String objectName) throws Exception {
        InputStream stream = customMinioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(objectName).build());
        return stream;
    }

    @SneakyThrows(Exception.class)
    public String getPreviewFileUrl(String bucket, String objectName) throws Exception {
        GetPresignedObjectUrlArgs args;
        if (objectName.contains("pdf")) {
            Map<String, String> headers = new HashMap<>();
            headers.put("response-content-type", "application/pdf");
            args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .method(Method.GET)
                    //.expiry(60) // 单位秒
                    .expiry(1800, TimeUnit.SECONDS)
                    .extraQueryParams(headers)
                    .build();
        } else {
            args = GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .method(Method.GET)
                    //.expiry(60) // 单位秒
                    .expiry(1800, TimeUnit.SECONDS)
                    .build();
        }
        return minioClientx.getPresignedObjectUrl(args);
    }
}
