package org.palladiosimulator.wlcgmodel;

public class NodeTypeDescription {
	
	private int count = 0;
	private String nodeName = null;
	private double computingRate = 0;
	private int jobSlots = 0;
	private int coreCount = 0;
	
	public NodeTypeDescription() {}

	public NodeTypeDescription(int count, String nodeName, double computingRate, int jobSlots, int coreCount) {
		super();
		this.count = count;
		this.nodeName = nodeName;
		this.computingRate = computingRate;
		this.jobSlots = jobSlots;
		this.coreCount = coreCount;
	}
}
