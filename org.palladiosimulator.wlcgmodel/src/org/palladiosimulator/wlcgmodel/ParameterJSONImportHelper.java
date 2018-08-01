package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This class provides functionality to load and parse model calibration parameter JSON files. JSON
 * handling is powered by Gson.
 *
 * @author Maximilian Stemmer-Grabow
 */
public class ParameterJSONImportHelper {

    // TODO Consolidate methods in a more generic way

    /**
     * Read and parse a JSON file that contains descriptions of node types.
     *
     * @param jsonFile
     *            The JSON file to read.
     * @return A list of node type description objects.
     * @throws IOException
     *             Thrown if reading the JSON file encounters a problem.
     * @throws JsonSyntaxException
     *             Thrown if the JSON file has an invalid structure.
     */
    public static List<NodeTypeDescription> readNodeTypes(File jsonFile) throws IOException, JsonSyntaxException {

        List<NodeTypeDescription> result = null;

        InputStream stream = new FileInputStream(jsonFile);
        Reader reader = new InputStreamReader(stream);

        Gson gson = new Gson();

        result = Arrays.asList(gson.fromJson(reader, NodeTypeDescription[].class));

        reader.close();
        return result;
    }

    /**
     * Read and parse a JSON file that contains descriptions of job types.
     *
     * @param jsonFile
     *            The JSON file to read.
     * @return A list of job type description objects.
     * @throws IOException
     *             Thrown if reading the JSON file encounters a problem.
     * @throws JsonSyntaxException
     *             Thrown if the JSON file has an invalid structure.
     */
    public static List<JobTypeDescription> readJobTypes(File jsonFile) throws IOException, JsonSyntaxException {

        List<JobTypeDescription> result = null;

        InputStream stream = new FileInputStream(jsonFile);
        Reader reader = new InputStreamReader(stream);

        Gson gson = new Gson();

        result = Arrays.asList(gson.fromJson(reader, JobTypeDescription[].class));

        reader.close();
        return result;
    }
}
