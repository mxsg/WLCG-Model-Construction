package org.palladiosimulator.wlcgmodel;

/**
 * Instances of this class contain the parameters required to describe the properties of a type of
 * node to be included in the resource environment of the simulation model.
 *
 * @author Maximilian Stemmer-Grabow
 *
 */
public class NodeTypeDescription {

    private String name = null;
    private double computingRate = 0.0;
    private int jobslots = 0;
    private int cores = 0;
    private int nodeCount = 0;

    /**
     * Construct a new job type description instance
     */
    public NodeTypeDescription() {
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the computingRate
     */
    public double getComputingRate() {
        return computingRate;
    }

    /**
     * @param computingRate
     *            the computingRate to set
     */
    public void setComputingRate(double computingRate) {
        this.computingRate = computingRate;
    }

    /**
     * @return the jobslots
     */
    public int getJobslots() {
        return jobslots;
    }

    /**
     * @param jobslots
     *            the jobslots to set
     */
    public void setJobslots(int jobslots) {
        this.jobslots = jobslots;
    }

    /**
     * @return the cores
     */
    public int getCores() {
        return cores;
    }

    /**
     * @param cores
     *            the cores to set
     */
    public void setCores(int cores) {
        this.cores = cores;
    }

    /**
     * @return the nodeCount
     */
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * @param nodeCount
     *            the nodeCount to set
     */
    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    @Override
    public String toString() {
        return "Name: " + this.name + ", " + this.cores + " cores," + this.jobslots + " jobslots," + this.nodeCount
                + " machines";
    }
}
