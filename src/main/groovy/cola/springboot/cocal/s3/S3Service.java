package cola.springboot.cocal.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3Service {
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // 사진 업로드
    public String uploadProfileImage(MultipartFile file, String fileName) throws IOException {
        String key = "profiles/" + fileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return getPublicUrl(key);
    }
    private String getPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    // 사진 삭제
    public void deleteFile(String key){
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    public void deleteIfOurS3Url(String url) {
        String key = keyFromUrl(url);
        if (key == null) return;          // 외부 링크거나 파싱 불가 → 삭제 스킵
        try {
            deleteFile(key);              // 없으면 404 나와도 예외 무시
        } catch (Exception ignore) {}
    }
    public String keyFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String noQuery = url.split("\\?")[0];
            var uri = java.net.URI.create(noQuery);
            String host = uri.getHost();      // ex) "<bucket>.s3.<region>.amazonaws.com"
            String path = uri.getPath();      // ex) "/profiles/xxx.png"
            if (host == null || path == null) return null;

            boolean hostMatch =
                    host.equals(bucket + ".s3." + region + ".amazonaws.com") ||
                            host.equals("s3." + region + ".amazonaws.com"); // 경로식 접근 대비
            if (!hostMatch || !path.startsWith("/profiles/")) return null;

            return path.substring(1);         // "profiles/xxx.png"
        } catch (Exception e) {
            return null;
        }
    }
}
