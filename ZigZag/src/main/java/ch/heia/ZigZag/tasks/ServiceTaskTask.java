package ch.heia.ZigZag.tasks;
import java.util.ArrayList;
import java.util.UUID;
public class ServiceTaskTask {

    private UUID id;
    private final ArrayList<String> dataIn = new ArrayList<>();

    private final ArrayList<String> dataOut = new ArrayList<>();

    private ServiceTaskBase.TaskStatus status;
    private UUID serviceId;
    private UUID pipelineExecutionID;

    public ServiceTaskTask(UUID id, ServiceTaskBase.TaskStatus status, UUID serviceId, UUID pipelineExecutionID) {
        this.id = id;
        this.status = status;
        this.serviceId = serviceId;
        this.pipelineExecutionID = pipelineExecutionID;
    }

    public ServiceTaskBase.TaskStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceTaskBase.TaskStatus status) {
        this.status = status;
    }
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ArrayList<String> getDataIn() {
        return dataIn;
    }

    public ArrayList<String> getDataOut() {
        return dataOut;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getPipelineId() {
        return pipelineExecutionID;
    }

    public void setPipelineId(UUID pipelineId) {
        this.pipelineExecutionID = pipelineId;
    }
}
