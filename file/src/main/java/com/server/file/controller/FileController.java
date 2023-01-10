package com.server.file.controller;

import com.server.file.dto.Fileinfo;
import com.server.file.entity.FileObject;
import com.server.file.service.FileService;
import com.server.file.util.ContentTypeSelector;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.transform.Result;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
@RestController
@RequestMapping(value = "/file")
public class FileController {

    private final FileService fileService;

    public FileController(@Qualifier("minioServiceImpl") FileService fileService) {
        this.fileService = fileService;
    }


    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public String upload(@RequestParam(value = "file") MultipartFile file,
                                 @RequestParam(value = "bucketName") String bucketName) {
        System.out.println("\nupload");
        return fileService.uploadFile(file, bucketName);
    }

    @PostMapping(value = "/uploadPath", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public String uploadWithPath(@RequestPart(value = "file") MultipartFile file,
                         @RequestParam(value = "bucketName") String bucketName,
                                 @RequestParam(value = "path") String path) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return fileService.uploadFileWithPath(file, bucketName, path);
    }

    @DeleteMapping("/remove")
    public String remove( @RequestBody FileObject fileObject) {
        fileService.removeFile(fileObject.getBucketName(), fileObject.getFileName());
        return "success";
    }

    @GetMapping("/getAll/")
    public List<Fileinfo> getAllFile(@RequestParam(value = "bucketName") String bucketName) throws Exception {
        return fileService.getObjectsFromABucket(bucketName);
    }


    @GetMapping("/previewPicture/{fileName}")
    public void previewPicture(@PathVariable("fileName") String objectName,
                               @RequestParam(value = "bucketName") String bucketName,
                               HttpServletResponse response) throws IOException {
        String fileType = objectName.substring(objectName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        String contentType = ContentTypeSelector.contentType.get(fileType);
        response.setContentType(contentType);
        try (ServletOutputStream out = response.getOutputStream()) {
            InputStream stream = fileService.getObject(bucketName, objectName);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = stream.read(buffer))) {
                output.write(buffer, 0, n);
            }
            byte[] bytes = output.toByteArray();
            out.write(bytes);
            out.flush();
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable("fileName") String objectName,
                                           @RequestParam(value = "bucketName") String bucketName) throws Exception {
        System.out.println(bucketName);
        ResponseEntity<byte[]> responseEntity = null;
        InputStream stream = null;
        ByteArrayOutputStream output = null;
        try {
            stream = fileService.getObject(bucketName, objectName);
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n = 0;
            while (-1 != (n = stream.read(buffer))) {
                output.write(buffer, 0, n);
            }
            byte[] bytes = output.toByteArray();

            //设置header
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Accept-Ranges", "bytes");
            httpHeaders.add("Content-Length", bytes.length + "");
            objectName = new String(objectName.getBytes(StandardCharsets.UTF_8), "ISO8859-1");
            //把文件名按UTF-8取出并按ISO8859-1编码
            httpHeaders.add("Content-disposition", "attachment; filename=" + objectName);
            httpHeaders.add("Content-Type", "text/plain;charset=utf-8");
            responseEntity = new ResponseEntity<byte[]>(bytes, httpHeaders, HttpStatus.CREATED);
        } finally {
            if (stream != null) {
                stream.close();
            }
            if (output != null) {
                output.close();
            }
        }
        return responseEntity;
    }

    @GetMapping(value = "/preview/{fileName}")
    public void getVideoOutStream(@PathVariable(value = "fileName") String fileName,
                                  @RequestParam(value = "bucketName") String bucketName,
                                  HttpServletRequest request, HttpServletResponse response) throws IOException {
        StatObjectResponse objectInfo = fileService.getObjectInfo(bucketName, fileName);
        String filenameExtension = StringUtils.getFilenameExtension(fileName).toLowerCase(Locale.ROOT);
        long fileSize = objectInfo.size();
        // Accept-Ranges: bytes
        response.setHeader("Accept-Ranges", "bytes");
        long startPos = 0;
        long endPos = fileSize - 1;
        String rangeHeader = request.getHeader("Range");
        if (!ObjectUtils.isEmpty(rangeHeader) && rangeHeader.startsWith("bytes=")) {

            try {
                // 情景一：RANGE: bytes=2000070- 情景二：RANGE: bytes=2000070-2000970
                String numRang = request.getHeader("Range").replaceAll("bytes=", "");
                if (numRang.startsWith("-")) {
                    endPos = fileSize - 1;
                    startPos = endPos - Long.parseLong(new String(numRang.getBytes(StandardCharsets.UTF_8), 1,
                            numRang.length() - 1)) + 1;
                } else if (numRang.endsWith("-")) {
                    endPos = fileSize - 1;
                    startPos = Long.parseLong(new String(numRang.getBytes(StandardCharsets.UTF_8), 0,
                            numRang.length() - 1));
                } else {
                    String[] strRange = numRang.split("-");
                    if (strRange.length == 2) {
                        startPos = Long.parseLong(strRange[0].trim());
                        endPos = Long.parseLong(strRange[1].trim());
                    } else {
                        startPos = Long.parseLong(numRang.replaceAll("-", "").trim());
                    }
                }

                if (startPos < 0 || endPos < 0 || endPos >= fileSize || startPos > endPos) {
                    // SC 要求的范围不满足
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                }

                // 断点续传 状态码206
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            } catch (NumberFormatException e) {
                startPos = 0;
            }
        }
        long rangLength = endPos - startPos + 1;
        response.setHeader("Content-Range", String.format("bytes %d-%d/%d", startPos, endPos, fileSize));
        response.addHeader("Content-Length", String.valueOf(rangLength));
        response.addHeader("Content-Type", ContentTypeSelector.contentType.get(filenameExtension));

            BufferedOutputStream bos = new BufferedOutputStream(response.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(fileService.getObject(bucketName, fileName));
            bis.skip(startPos);
            IOUtils.copy(bis, bos);
    }
}
