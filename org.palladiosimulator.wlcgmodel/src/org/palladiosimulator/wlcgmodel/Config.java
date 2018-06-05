package org.palladiosimulator.wlcgmodel;

public class Config {
    
    public static final String MODEL_BLUEPRINTS_FOLDER = "platform:/plugin/org.palladiosimulator.wlcgmodel/blueprint-wlcg";
    
    // Default values for the parameter files
    // TODO Make this configurable or selectable in the UI
    public static final String NODE_DESCRIPTION_FILENAME = "nodes.json";
    public static final String JOB_DESCRIPTION_FILENAME = "jobs.json";
    
    /**
     * Do not allow instantiation of utility class.
     */
    private Config() {
    }

}
