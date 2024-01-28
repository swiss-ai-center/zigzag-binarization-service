package ch.heia.ZigZag.service;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

/**
 * StorageService handles image uploading and downloading from the storage server.
 */
@Service
public class StorageService {

    private final Logger logger = LoggerFactory.getLogger(TaskService.class);

    /**
     * Uploads a file to the storage service.
     * @param uploadFile The file to upload (bytes)
     * @param fileExtensionWithDot The file extension (with the dot)
     * @param regionName The region name of the S3 server
     * @param secretAccessKey The secret access key of the S3 server
     * @param accessKeyId The access key id of the S3 server
     * @param endpointUrl The endpoint url of the S3 server
     * @param bucket The bucket name on the S3 server
     */
    public String uploadSynchronous(
            byte[] uploadFile,
            String fileExtensionWithDot,
            String regionName,
            String secretAccessKey,
            String accessKeyId,
            String endpointUrl,
            String bucket
    ) {
        String key = UUID.randomUUID() + fileExtensionWithDot;

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        S3Configuration conf = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(regionName))
                .endpointOverride(URI.create(endpointUrl))
                .serviceConfiguration(conf)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build()) {

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            logger.info("About to request to put file on s3:" + key);
            s3Client.putObject(putObjectRequest, RequestBody.fromByteBuffer(ByteBuffer.wrap(uploadFile)));
        } catch (Exception e) {
            logger.error("Error uploading file: " + e.getMessage());
            return null;
        }
        return key;
    }

    /**
     * Downloads a file from the storage service.
     * @param key The requested file's key
     * @param regionName The region name of the S3 server
     * @param secretAccessKey The secret access key of the S3 server
     * @param accessKeyId The access key id of the S3 server
     * @param endpointUrl The endpoint url of the S3 server
     * @param bucket The bucket name on the S3 server
     */
    public byte[] getFileSynchronous(
            String key,
            String regionName,
            String secretAccessKey,
            String accessKeyId,
            String endpointUrl,
            String bucket
    ) {

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        logger.info("getFile called with parameters: key=" + key + "\n, regionName=" + regionName + "\n secretAccessKey=" + secretAccessKey +
                "\n accessKeyId=" + accessKeyId + "\n endpointUrl=" + endpointUrl + "\n bucket=" + bucket);
        S3Configuration conf = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        logger.info("actual endpoint used :" + URI.create(endpointUrl));
        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(regionName))
                .endpointOverride(URI.create(endpointUrl))
                .serviceConfiguration(conf)
                .credentialsProvider(() -> credentials)
                .build()) {

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            logger.info("Requesting object from S3: " + key);
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);

            return responseBytes.asByteArray();
        } catch (Exception ex) {
            logger.error("Error getting file: " + ex.getMessage());
            return null;
        }
    }
}
