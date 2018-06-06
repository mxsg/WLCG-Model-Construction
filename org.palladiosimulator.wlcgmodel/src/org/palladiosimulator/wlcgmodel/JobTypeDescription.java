package org.palladiosimulator.wlcgmodel;

public class JobTypeDescription {
    private String typeName = null;
    private String interarrivalStoEx = null;
    private String cpuDemandStoEx = null;
    private String ioTimeStoEx = null;
    private String requiredJobslotsStoEx = null;

    public JobTypeDescription() {
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getInterarrivalStoEx() {
        return interarrivalStoEx;
    }

    public void setInterarrivalStoEx(String interarrivalStoEx) {
        this.interarrivalStoEx = interarrivalStoEx;
    }

    public String getCpuDemandStoEx() {
        return cpuDemandStoEx;
    }

    public void setCpuDemandStoEx(String cpuDemandStoEx) {
        this.cpuDemandStoEx = cpuDemandStoEx;
    }

    public String getIoTimeStoEx() {
        return ioTimeStoEx;
    }

    public void setIoTimeStoEx(String ioTimeStoEx) {
        this.ioTimeStoEx = ioTimeStoEx;
    }

    public String getRequiredJobslotsStoEx() {
        return requiredJobslotsStoEx;
    }

    public void setRequiredJobslotsStoEx(String requiredJobslotsStoEx) {
        this.requiredJobslotsStoEx = requiredJobslotsStoEx;
    }

    public String toString() {
        return "Job Type";
    }
}
