package ch.heia.ZigZag.service;

import ch.heia.ZigZag.tasks.ServiceTaskBase;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TaskService is dedicated to processing binarization tasks. A task has an image input, this image is to be downloaded
 * from the storage before being binarized and then re-uploaded to the storage as output. The core engine is then
 * notified that the task is finished.
 */
@Service
public class TaskService {

    private static final String SERVICE_URL = System.getenv("SERVICE_URL") != null ? System.getenv("SERVICE_URL") : "http://localhost:80";
    private static final String SERVICE_NAME = "zigzag-binarization";
    private final BinarizationService binarizationService;
    private final StorageService storageService;

    private final Logger logger = LoggerFactory.getLogger(TaskService.class);

    /**
     * computingThread is the thread responsible for processing tasks.
     */
    private Thread computingThread;

    /**
     * taskQueue contains all tasks that have yet to be processed.
     */
    private final LinkedBlockingQueue<ServiceTaskBase> taskQueue = new LinkedBlockingQueue<>();

    /**
     * currentTaskBinarizedImg contains the images already processed (binarized) for the current task.
     */
    private final ArrayList<BufferedImage> currenTaskBinarizedImg = new ArrayList<>();

    /**
     * currentTaskImages contains images that the current task has yet to process.
     */
    private final HashMap<String, byte[]> currentTaskImages = new HashMap<>();

    /**
     * UnfinishedTasks contains all tasks that have not been finished correctly.
     */
    private final LinkedBlockingQueue<ServiceTaskBase> unfinishedTasks = new LinkedBlockingQueue<>();
    private final static AtomicBoolean running = new AtomicBoolean(false);

    private final ReentrantLock currentTaskLock = new ReentrantLock();
    private static ServiceTaskBase currentTask = null;

    @Autowired
    public TaskService(StorageService storageService, BinarizationService binarizationService) {
        this.storageService = storageService;
        this.binarizationService = binarizationService;
        logger.info(SERVICE_NAME + " service started");
        logger.info("Service URL: " + SERVICE_URL);
    }


    /**
     * getTaskStatus returns the status of the given task of it exists, either in the taskQueue (not processed yet),
     * in the unfinished tasks (error occurred), or the current task.
     * @param taskId the uuid of the requested task
     * @return the status of the task if it exists, null otherwise.
     */
    public ServiceTaskBase.TaskStatus getTaskStatus(UUID taskId) {
        currentTaskLock.lock();
        try {
            if (currentTask != null && currentTask.getTask().getId() == taskId)
                return currentTask.getTask().getStatus();

            ServiceTaskBase task = findInQueue(taskId, taskQueue);
            if (task != null) return task.getTask().getStatus();
            task = findInQueue(taskId, unfinishedTasks);
            if (task != null) return task.getTask().getStatus();

            return null;
        } finally {
            currentTaskLock.unlock();
        }
    }

    /**
     * findInQueue is a helper function to find a task in a given queue with a given uuid.
     * @param taskId the searched task's uuid
     * @param queue the queue the task will be searched in
     * @return the task with the corresponding uuid if it exists, null otherwise.
     */
    private ServiceTaskBase findInQueue(UUID taskId, LinkedBlockingQueue<ServiceTaskBase> queue) {
            for (ServiceTaskBase task: queue) {
                if (task.getTask().getId().equals(taskId)) return task;
            }
        return null;
    }


    /**
     * addTask adds a task to the taskQueue (tasks that have yet to be processed).
     * @param task the task to add
     */
    public void addTask(ServiceTaskBase task) {
        taskQueue.add(task);
        logger.info("Task  " + task.getTask().getId() + "has been added to the task queue");
        start();
    }

    /**
     * start creates a new thread (if it doesn't exist yet) which executes the
     * run() method. It starts the processing of tasks in the task queue.
     */
    public void start() {
        running.set(true);
        if (computingThread == null) {
            computingThread = new Thread(this::run);
            computingThread.start();
        }
    }

    /**
     * initTask takes a task in the task queue and gets all the related data (should be one image)
     * from the storage (data_in).
     * @return true if the method was successful (no exceptions), false otherwise.
     */
    public boolean initTask() {
        try {
            logger.info("Getting next task from task queue");
            ServiceTaskBase newTask = taskQueue.take();
            currentTaskLock.lock();
            currentTask = newTask;
            currentTask.getTask().setStatus(ServiceTaskBase.TaskStatus.FETCHING);
            logger.info("dataIn: " + currentTask.getTask().getDataIn().toString());
            for (String file : currentTask.getTask().getDataIn()) {
                if (!file.endsWith(".jpeg") && !file.endsWith(".png") && !file.endsWith(".jpg"))
                    throw new IllegalArgumentException("Wrong file extension, expected  image/png or image/jpeg got: " + file);

                logger.info("File extension ok, calling storage service to download files");
                byte[] image = storageService.getFileSynchronous(file, currentTask.getS3Region(),
                        currentTask.getS3SecretAccessKey(), currentTask.getS3AccessKeyId(), currentTask.getS3Host(),
                        currentTask.getS3Bucket());

                if (image == null)
                    throw new IllegalStateException("Could not download image");

                logger.info("Got image from s3 adding to taskImages :" + file);
                currentTaskImages.put(file , image);
            }
        } catch ( Exception e) {
            logger.error(e.getMessage());
            logger.info("Error while initiating task");
            handleTaskError();
            return false;
        } finally {
            currentTaskLock.unlock();
        }
        return true;
    }

    /**
     * processTask takes the images in the currentTaskImages array and applies binarization.
     * @return true if the method was successful (no exceptions), false otherwise.
     */
    private boolean processTask() {
        currentTaskLock.lock();
        currentTask.getTask().setStatus(ServiceTaskBase.TaskStatus.PROCESSING);
        currentTaskLock.unlock();


        for (Map.Entry<String, byte[]> image : currentTaskImages.entrySet()) {
            try {
                logger.info("Getting bytes from image: " + image.getKey());
                byte[] imageBytes = image.getValue();
                logger.info("got bytes from downloaded file, proceeding with binarization");
                BufferedImage binarizedImage = binarizationService.binarizeImageBytes(imageBytes,
                        BinarizationService.DEFAULT_BIN_MODE, BinarizationService.DEFAULT_WINDOW_SIZE);
                currenTaskBinarizedImg.add(binarizedImage);
                logger.info("binarization complete for image: " + image.getKey());

            } catch (Exception e ) {
                logger.info("problem during processing of the image: " + e.getMessage());
                logger.info(e.getMessage());
                handleTaskError();
                return false;
            }
        }
        return true;
    }

    /**
     * endTask saves the binarized images (PNG format) in the storage.
     * @return true if the method was successful (no exceptions), false otherwise.
     */
    private boolean endTask() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        currentTaskLock.lock();
        try {
            for (BufferedImage image: currenTaskBinarizedImg) {
                ImageIO.write(image,"png", baos);
                String key = storageService.uploadSynchronous(baos.toByteArray(), ".png", currentTask.getS3Region(),
                        currentTask.getS3SecretAccessKey(), currentTask.getS3AccessKeyId(), currentTask.getS3Host(),
                        currentTask.getS3Bucket());
                logger.info("stored image" + key);
                currentTask.getTask().getDataOut().add(key);
            }
            currentTask.getTask().setStatus(ServiceTaskBase.TaskStatus.FINISHED);
        } catch (IOException e) {
            handleTaskError();
            logger.info("Failed to upload image");
            logger.error(e.getMessage());
            return false;
        } finally {
            currentTaskLock.unlock();
        }
        return true;
    }

    /**
     * notifyEngine sends an http patch request to the core engine to notify that the task is finished.
     * @return true if the method was successful (no exceptions), false otherwise.
     */
    private boolean notifyEngine() {
        JSONObject payload = new JSONObject();
        payload.put("service", SERVICE_NAME);
        payload.put("url", SERVICE_URL);
        currentTaskLock.lock();
        try {
            payload.put("data_out", currentTask.getTask().getDataOut());
            payload.put("status", currentTask.getTask().getStatus().toString().toLowerCase());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(currentTask.getCallbackUrl()))
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .header("Content-Type", "application/json")
                    .build();
            logger.info("Patching task on core engine, sent request: " + request.bodyPublisher().toString());
            HttpResponse<String> response  = HttpClient.newBuilder()
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("sent PATCH to core engine: response: " + response.body());
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
            handleTaskError();
            return false;
        } finally {
            currentTaskLock.unlock();
        }
        currenTaskBinarizedImg.clear();
        currentTaskImages.clear();
        return true;
    }

    /**
     * the run method processes tasks in the task queue. If an error occurs during the operation,
     * move on to the next task.
     */
    private void run() {
        while (running.get()) {
            if (!(initTask() && processTask() && endTask() && notifyEngine())) continue;
        }
    }

    /**
     * handleTaskError sets the status of the current task to ERROR adds it to
     * the unfinished tasks and resets the image arrays.
     */
    private void handleTaskError() {
        currenTaskBinarizedImg.clear();
        currentTaskImages.clear();
        currentTaskLock.lock();
        currentTask.getTask().setStatus(ServiceTaskBase.TaskStatus.ERROR);
        unfinishedTasks.add(currentTask);
        currentTask = null;
        currentTaskLock.unlock();
    }
}
