package org.palladiosimulator.wlcgmodel;

/**
 * Instances of this class contain the parameters required to describe the properties
 * of a type of job to be included in the simulation model.
 *
 * @author Maximilian Stemmer-Grabow
 *
 */
public class JobTypeDescription {

    private String typeName = null;
    private String interarrivalStoEx = null;
    private String cpuDemandStoEx = null;
    private String ioTimeStoEx = null;
    private String requiredJobslotsStoEx = null;
    private double relativeFrequency = 0.0;
    private String ioTimeRatioStoEx = null;
    private int schedulingDelay = 12000;

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

    public double getRelativeFrequency() {
        return relativeFrequency;
    }

    public void setRelativeFrequency(double relativeFrequency) {
        this.relativeFrequency = relativeFrequency;
    }

    public String getIoTimeRatioStoEx() {
        return ioTimeRatioStoEx;
    }

    public void setIoTimeRatioStoEx(String ioTimeRatioStoEx) {
        this.ioTimeRatioStoEx = ioTimeRatioStoEx;
    }

    @Override
    public String toString() {
        return "Job Type";
    }

    public int getSchedulingDelay() {
        return schedulingDelay;
    }

    public void setSchedulingDelay(int schedulingDelay) {
        this.schedulingDelay = schedulingDelay;
    }
}
