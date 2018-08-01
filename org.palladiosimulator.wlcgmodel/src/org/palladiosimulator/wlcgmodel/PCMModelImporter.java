package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.commons.eclipseutils.FileHelper;

import com.google.gson.JsonSyntaxException;

/**
 * @author Maximilian Stemmer-Grabow
 *
 */
public class PCMModelImporter {

    public static boolean importAndCompleteBlueprintModel(IProject project, File nodeDescriptionFile,
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

        // Copying original blueprint to project location
        //
        try {
            addToProject(computeBlueprintPath(), project, null);
        } catch (CoreException e) {
            System.out.println("Something went wrong when importing blueprint model files.");
            return false;
        }

        // Compute project location
        URI projectURI = URI.createURI(project.getFullPath().toString());

        BlueprintModelCompletion completion = new BlueprintModelCompletion();

        completion.completeModels(projectURI, nodes, jobs);
        return true;
    }

    private static void addToProject(final URI path, final IContainer target, final SubMonitor subMonitor)
            throws CoreException {
        for (final File source : FileHelper.getFiles(path.toString())) {
            final IPath newTarget = new Path(source.getName());
            if (source.isDirectory()) {
                addFolderToProject(path, source, target.getFolder(newTarget), subMonitor);
            } else {
                addFileToProject(source, target.getFile(newTarget), subMonitor);
            }
        }
    }

    private static void addFolderToProject(final URI path, final File source, final IFolder target,
            final SubMonitor subMonitor) throws CoreException {
        if (!target.exists()) {
            target.create(IResource.NONE, true, null);
        }

        addToProject(path.appendSegment(source.getName()), target, subMonitor);
    }

    private static void addFileToProject(final File source, final IFile target, final SubMonitor subMonitor)
            throws CoreException {
        try (final InputStream contentStream = new FileInputStream(source)) {
            if (target.exists()) {
                target.setContents(contentStream, true, true, subMonitor);
            } else {
                target.create(contentStream, true, subMonitor);
            }
        } catch (final FileNotFoundException e) {
            throwCoreException("File " + source.getAbsolutePath() + " does not exist!");
        } catch (final IOException e) {
            throwCoreException(
                    "Cannot create input stream on file " + source.getAbsolutePath() + "! " + e.getMessage());
        } catch (final CoreException e) {
            throwCoreException(e.getMessage());
        }
    }

    private static URI computeBlueprintPath() {
        return URI.createURI(Config.MODEL_BLUEPRINTS_DIRECTORY);
    }

    /**
     * Throw a core exception based on a given error message.
     *
     * @param message
     *            The message to present.
     * @throws CoreException
     *             The exception to throw.
     */
    private static void throwCoreException(final String message) throws CoreException {
        final IStatus status = new Status(IStatus.ERROR, "org.palladiosimulator.wlcgmodel", IStatus.OK, message, null);
        throw new CoreException(status);
    }
}
