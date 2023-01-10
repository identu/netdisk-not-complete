package com.server.file.service;

import com.server.file.dto.Fileinfo;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface FileService {

    String uploadFile(MultipartFile file, String bucketName);

    void removeFile(String bucketName, String objectName);

    InputStream getObject(String bucketName, String objectName);

    public StatObjectResponse getObjectInfo(String bucketName, String objectName);

    List<Fileinfo> getObjectsFromABucket(String bucketName) throws Exception;

    String uploadFileWithPath(MultipartFile file, String bucketName, String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}
