package ch.heia.ZigZag.controller;

import ch.heia.ZigZag.service.TaskService;
import ch.heia.ZigZag.tasks.ServiceTaskBase;
import ch.heia.ZigZag.tasks.ServiceTaskTask;
import org.json.JSONArray;
import org.json.JSONObject;
import ch.heia.ZigZag.tasks.ServiceTaskBase.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Controller
public class CoreEngineController {


    private final TaskService taskService;
    @Autowired
    private CoreEngineController(TaskService service) {
        this.taskService = service;
    }
    Logger logger = LoggerFactory.getLogger(CoreEngineController.class);
    @RequestMapping(value = "${UrlPrefix}/status", method = RequestMethod.GET)
    @ResponseBody
    public String getStatus() {
        logger.info("Status route called");
        return "Ok";
    }


    @RequestMapping(value = "${UrlPrefix}/tasks/{task_id}/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<HashMap<String,String>> getTaskStatus(@PathVariable("task_id") String id) {
        UUID uuid = UUID.fromString(id);
        logger.info("task status route called, id :" + id );
        ServiceTaskBase.TaskStatus  status = taskService.getTaskStatus(uuid);
        HashMap<String,String> response = new HashMap<>();

        if (status == null) {
            logger.info("Task " + id + "  not found");
            response.put("detail", "Task " + id + " not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        response.put("status", status.getValue());
        logger.info("Task status is " + status.getValue());
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "${UrlPrefix}/compute", method = RequestMethod.POST)
    public ResponseEntity<String> compute(@RequestBody String body) {
        JSONObject json = new JSONObject(body);

        // Parse the Json body to create the Task object
        JSONObject task = (JSONObject) json.get("task");
        UUID id = UUID.fromString((String) task.get("id"));
        TaskStatus status = TaskStatus.valueOf(task.get("status").toString().toUpperCase());
        UUID serviceId = UUID.fromString((String) task.get("service_id"));
        UUID pipelineExecId = null;
        if (!task.isNull("pipeline_execution_id"))
            pipelineExecId = UUID.fromString((String) task.get("pipeline_execution_id"));
        ServiceTaskTask serviceTaskTask = new ServiceTaskTask(id, status, serviceId, pipelineExecId);
        JSONArray jsonDataIn = (JSONArray) task.get("data_in");
        ArrayList<String> dataIn = serviceTaskTask.getDataIn();

        for (int i = 0; i < jsonDataIn.length(); i++) {
            dataIn.add((String)jsonDataIn.get(i));
        }
        String accessKeyId, secretAccessKey, region, host, bucket, callbackUrl;
        accessKeyId = (String) json.get("s3_access_key_id");
        secretAccessKey = (String) json.get("s3_secret_access_key");
        region = (String) json.get("s3_region");
        host = (String) json.get("s3_host");
        bucket = (String) json.get("s3_bucket");
        callbackUrl = (String) json.get("callback_url");
        ServiceTaskBase serviceTaskBase = new ServiceTaskBase(accessKeyId,secretAccessKey, region, host, bucket, serviceTaskTask,
                callbackUrl);

        logger.info("created TaskBase from json body, id : " + serviceTaskBase.getTask().getId());
        taskService.addTask(serviceTaskBase);
        return ResponseEntity.ok().build();
    }
}