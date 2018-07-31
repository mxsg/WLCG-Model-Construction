package org.palladiosimulator.wlcgmodel;

/**
 * This class contains project-wide configuration attributes.
 *
 * @author Maximilian Stemmer-Grabow
 *
 */
public class Config {


    /** The platform-relative directory that hold the blueprint simulation models. */
    public static final String MODEL_BLUEPRINTS_DIRECTORY = "platform:/plugin/org.palladiosimulator.wlcgmodel/blueprint-wlcg";

    // TODO Future work: Make this configurable or selectable in the UI

    /** Default value for the node description file to be used to calibrate the model. */
    public static final String NODE_DESCRIPTION_FILENAME = "nodes.json";

    /** Default value for the job type description file to be used to calibrate the model. */
    public static final String JOB_DESCRIPTION_FILENAME = "jobs.json";

    /**
     * Do not allow instantiation of utility class.
     */
    private Config() {
    }
}
