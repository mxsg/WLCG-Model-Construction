package org.palladiosimulator.wlcgmodel;

/**
 * Instances of this class contain the parameters required to describe the properties of a type of
 * job to be included in the simulation model.
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
    private int schedulingDelay = 0;
    private boolean useIoRatio = false;
    private String resourceDemandRounds = "10";

    /**
     * Construct a new job type description instance
     */
    public JobTypeDescription() {
    }

    /**
     * @return the typeName
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @param typeName
     *            the typeName to set
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    /**
     * @return the interarrivalStoEx
     */
    public String getInterarrivalStoEx() {
        return interarrivalStoEx;
    }

    /**
     * @param interarrivalStoEx
     *            the interarrivalStoEx to set
     */
    public void setInterarrivalStoEx(String interarrivalStoEx) {
        this.interarrivalStoEx = interarrivalStoEx;
    }

    /**
     * @return the cpuDemandStoEx
     */
    public String getCpuDemandStoEx() {
        return cpuDemandStoEx;
    }

    /**
     * @param cpuDemandStoEx
     *            the cpuDemandStoEx to set
     */
    public void setCpuDemandStoEx(String cpuDemandStoEx) {
        this.cpuDemandStoEx = cpuDemandStoEx;
    }

    /**
     * @return the ioTimeStoEx
     */
    public String getIoTimeStoEx() {
        return ioTimeStoEx;
    }

    /**
     * @param ioTimeStoEx
     *            the ioTimeStoEx to set
     */
    public void setIoTimeStoEx(String ioTimeStoEx) {
        this.ioTimeStoEx = ioTimeStoEx;
    }

    /**
     * @return the requiredJobslotsStoEx
     */
    public String getRequiredJobslotsStoEx() {
        return requiredJobslotsStoEx;
    }

    /**
     * @param requiredJobslotsStoEx
     *            the requiredJobslotsStoEx to set
     */
    public void setRequiredJobslotsStoEx(String requiredJobslotsStoEx) {
        this.requiredJobslotsStoEx = requiredJobslotsStoEx;
    }

    /**
     * @return the relativeFrequency
     */
    public double getRelativeFrequency() {
        return relativeFrequency;
    }

    /**
     * @param relativeFrequency
     *            the relativeFrequency to set
     */
    public void setRelativeFrequency(double relativeFrequency) {
        this.relativeFrequency = relativeFrequency;
    }

    /**
     * @return the ioTimeRatioStoEx
     */
    public String getIoTimeRatioStoEx() {
        return ioTimeRatioStoEx;
    }

    /**
     * @param ioTimeRatioStoEx
     *            the ioTimeRatioStoEx to set
     */
    public void setIoTimeRatioStoEx(String ioTimeRatioStoEx) {
        this.ioTimeRatioStoEx = ioTimeRatioStoEx;
    }

    /**
     * @return the schedulingDelay
     */
    public int getSchedulingDelay() {
        return schedulingDelay;
    }

    /**
     * @param schedulingDelay
     *            the schedulingDelay to set
     */
    public void setSchedulingDelay(int schedulingDelay) {
        this.schedulingDelay = schedulingDelay;
    }

    /**
     * @return the useIoRatio
     */
    public boolean getUseIoRatio() {
        return useIoRatio;
    }

    /**
     * @param useIoRatio
     *            the useIoRatio to set
     */
    public void setUseIoRatio(boolean useIoRatio) {
        this.useIoRatio = useIoRatio;
    }

    /**
     * @return the resourceDemandRounds
     */
    public String getResourceDemandRounds() {
        return resourceDemandRounds;
    }

    /**
     * @param resourceDemandRounds
     *            the resourceDemandRounds to set
     */
    public void setResourceDemandRounds(String resourceDemandRounds) {
        this.resourceDemandRounds = resourceDemandRounds;
    }

    @Override
    public String toString() {
        return "Job Type:" + this.typeName;
    }
}
