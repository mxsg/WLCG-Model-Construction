package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.wlcgmodel.util.ProjectCreationHelper;

import com.google.gson.JsonSyntaxException;

/**
 * This class provides functionality to import and complete blueprint simulation models.
 *
 * @author Maximilian Stemmer-Grabow
 *
 */
public class BlueprintModelImport {

    /**
     * Import models from supplied path to the project, load parameter set and complete simulation
     * model.
     *
     * @param project
     *            The project the simulation model will be created in.
     * @param blueprintPath
     *            The path to the original blueprint models.
     * @param nodeDescriptionFile
     *            The file containing the node descriptions in JSON format.
     * @param jobDescriptionFile
     *            The file containing job descriptions in JSON format.
     * @return True if the model completion succeeds, else false.
     */
    public static boolean importAndCompleteBlueprintModel(IProject project, URI blueprintPath, File nodeDescriptionFile,
            File jobDescriptionFile) {

        // Find parameter files and import data

        List<NodeTypeDescription> nodes = null;
        try {
            nodes = ParameterJSONImportHelper.readNodeTypes(nodeDescriptionFile);
        } catch (IOException e) {
            System.out.println("Could not read node type file: " + nodeDescriptionFile);
            return false;
        } catch (JsonSyntaxException e) {
            System.out.println("File " + nodeDescriptionFile + " does not have correct JSON syntax:" + e);
            return false;
        }

        if (nodes == null) {
            System.out.println("Something went wrong when importing jobs types!");
            return false;
        }

        List<JobTypeDescription> jobs = null;
        try {
            jobs = ParameterJSONImportHelper.readJobTypes(jobDescriptionFile);
        } catch (IOException e) {
            System.out.println("Could not read job type file: " + jobDescriptionFile);
            return false;
        } catch (JsonSyntaxException e) {
            System.out.println("File " + jobDescriptionFile + " does not have correct JSON syntax:" + e);
            return false;
        }

        if (jobs == null) {
            System.out.println("Something went wrong when importing jobs types!");
            return false;
        }

        // Copy original blueprint to project location
        try {
            ProjectCreationHelper.addToProject(blueprintPath, project, null);
        } catch (CoreException e) {
            System.out.println("Something went wrong when importing blueprint model files.");
            return false;
        }

        // Compute project location
        URI projectURI = URI.createURI(project.getFullPath().toString());

        WLCGModelConstructor completion = new WLCGModelConstructor();
        completion.completeModels(projectURI, nodes, jobs);

        return true;
    }
}
