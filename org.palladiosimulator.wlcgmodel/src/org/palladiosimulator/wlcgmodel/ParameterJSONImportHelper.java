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

    /**
     * Read and parse a JSON file that contains descriptions of a certain parameter set type.
     *
     * @param jsonFile
     *            The JSON file to read.
     * @param classOfT
     *            Class of the description file to read.
     * @param <T>
     *            Class of the description file to be read.
     * @return A list of job type description objects, or null if the import failed.
     */
    public static <T> List<T> readParameterFile(File jsonFile, Class<T> classOfT) {

        List<T> result = null;
        try {
            InputStream stream = new FileInputStream(jsonFile);
            Reader reader = new InputStreamReader(stream);

            Gson gson = new Gson();
            try {
                result = Arrays.asList(gson.fromJson(reader, classOfT));
            } catch (JsonSyntaxException e) {
                System.out.println("File " + jsonFile + " does not have correct JSON syntax:" + e);
                return null;
            }

            reader.close();

        } catch (IOException e) {
            System.out.println("Could not read node type file: " + jsonFile);
            result = null;
        }

        return result;
    }
}
