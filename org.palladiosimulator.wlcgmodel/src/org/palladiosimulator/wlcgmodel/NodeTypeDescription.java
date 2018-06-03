package org.palladiosimulator.wlcgmodel;

public class NodeTypeDescription {
	
	private String name = null;
	private double computingRate = 0.0;
	private int jobslots = 0;
	private int cores = 0;
	private int nodeCount = 0;
	
	public NodeTypeDescription() {}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getComputingRate() {
		return computingRate;
	}

	public void setComputingRate(double computingRate) {
		this.computingRate = computingRate;
	}

	public int getJobslots() {
		return jobslots;
	}

	public void setJobslots(int jobslots) {
		this.jobslots = jobslots;
	}

	public int getCores() {
		return cores;
	}

	public void setCores(int cores) {
		this.cores = cores;
	}

	public int getNodeCount() {
		return nodeCount;
	}

	public void setNodeCount(int nodeCount) {
		this.nodeCount = nodeCount;
	}
	
	@Override
	public String toString() {
		return "Name: " + name + ", " + cores + " cores," + jobslots + " jobslots,"+ nodeCount + " machines";
	}
}
