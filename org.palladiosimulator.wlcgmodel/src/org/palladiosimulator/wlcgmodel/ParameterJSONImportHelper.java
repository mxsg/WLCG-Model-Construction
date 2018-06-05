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

public class ParameterJSONImportHelper {

    // TODO Consolidate methods in a more generic way

    public static List<NodeTypeDescription> readNodeTypes(File jsonFile) throws IOException, JsonSyntaxException {

        List<NodeTypeDescription> result = null;

        InputStream stream = new FileInputStream(jsonFile);
        Reader reader = new InputStreamReader(stream);

        Gson gson = new Gson();

        result = Arrays.asList(gson.fromJson(reader, NodeTypeDescription[].class));

        reader.close();
        return result;
    }

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
