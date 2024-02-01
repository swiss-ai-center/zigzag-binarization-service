package ch.heia.ZigZag.tasks;

public class ServiceTaskBase {

    /**
     * Base class for Service task
     * This model is used in subclasses
     */
    private String s3AccessKeyId;
    private String s3SecretAccessKey;
    private String s3Region;
    private String s3Host;
    private String s3Bucket;
    private ServiceTaskTask task;
    private String callbackUrl;

    public ServiceTaskBase(String s3AccessKeyId, String s3SecretAccessKey, String s3Region, String s3Host, String s3Bucket, ServiceTaskTask task, String callbackUrl) {
        this.s3AccessKeyId = s3AccessKeyId;
        this.s3SecretAccessKey = s3SecretAccessKey;
        this.s3Region = s3Region;
        this.s3Host = s3Host;
        this.s3Bucket = s3Bucket;
        this.task = task;
        this.callbackUrl = callbackUrl;
    }

    public enum TaskStatus {
        PENDING("pending"),
        FETCHING("fetching"),
        PROCESSING("processing"),
        SAVING("saving"),
        FINISHED("finished"),
        ERROR("error"),
        UNAVAILABLE("unavailable");

        private final String label;
        TaskStatus(String label) {
            this.label = label;
        }

        public String getValue() {
            return label;
        }
    }

    public String getS3AccessKeyId() {
        return s3AccessKeyId;
    }

    public String getS3SecretAccessKey() {
        return s3SecretAccessKey;
    }

    public String getS3Region() {
        return s3Region;
    }

    public String getS3Host() {
        return s3Host;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public ServiceTaskTask getTask() {
        return task;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }
}
