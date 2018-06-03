package org.palladiosimulator.wlcgmodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;


public class ParameterJSONImportHelper {
	
	public static List<NodeTypeDescription> readNodeTypes(File jsonFile) throws FileNotFoundException, IOException {
		
        BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonFile));
        Gson gson = new Gson();
        
        NodeTypeDescription[] nodesArray = gson.fromJson(bufferedReader, NodeTypeDescription[].class);
		
		bufferedReader.close();
		return Arrays.asList(nodesArray);
	}
	
	public static List<JobTypeDescription> readJobTypes(File jsonFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonFile));
        Gson gson = new Gson();
        
        JobTypeDescription[] nodesArray = gson.fromJson(bufferedReader, JobTypeDescription[].class);
		
		bufferedReader.close();
		return Arrays.asList(nodesArray);
	}
}
