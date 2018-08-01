package org.palladiosimulator.wlcgmodel;

import org.eclipse.emf.common.util.URI;

/**
 * This class contains project-wide configuration attributes.
 *
 * @author Maximilian Stemmer-Grabow
 *
 */
public final class Config {

    /** The plugin ID */
    public static final String PLUGIN_ID = "org.palladiosimulator.wlcgmodel";

    /** The platform-relative directory that hold the blueprint simulation models. */
    public static final String MODEL_BLUEPRINTS_DIRECTORY = "platform:/plugin/org.palladiosimulator.wlcgmodel/blueprint-wlcg";

    /** The path to the simulation model blueprint. */
    public static final URI MODEL_BLUEPRINT_URI = URI.createURI(MODEL_BLUEPRINTS_DIRECTORY);

    /** Path to the directory containing the blueprint model files. */
    public static final String MODEL_PARAMETER_FOLDER = "platform:/plugin/org.palladiosimulator.wlcgmodel/parameters";

    // TODO Future work: Make this configurable or selectable in the UI

    /** Default value for the node description file to be used to calibrate the model. */
    public static final String NODE_DESCRIPTION_FILENAME = "nodes.json";

    /** Default value for the job type description file to be used to calibrate the model. */
    public static final String JOBS_DESCRIPTION_FILENAME = "jobs.json";

    /**
     * Do not allow instantiation of utility class.
     */
    private Config() {
    }
}
