package org.palladiosimulator.wlcgmodel.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.commons.eclipseutils.FileHelper;
import org.palladiosimulator.wlcgmodel.Config;

/**
 * This class contains convenience methods to support the creation of new Eclipse projects.
 *
 * @author Maximilian Stemmer-Grabow
 *
 */
public class ProjectCreationHelper {

    /**
     * Add the resources (files and directories) at the supplied path to the target project
     * recursively.
     *
     * Adapted from the generic sirius editor Palladio modeling project creation wizard:
     * {@link org.palladiosimulator.editors.sirius.ui.wizard.project.NewPalladioProjectWizard}.
     *
     * @param path
     *            The path of the source files.
     * @param target
     *            The target project.
     * @param subMonitor
     *            The progress monitor to be updated.
     * @throws CoreException
     *             Thrown if an error occurs during copying and adding the resources to the project.
     */
    public static void addToProject(final URI path, final IContainer target, final SubMonitor subMonitor)
            throws CoreException {
        for (final File source : FileHelper.getFiles(path.toString())) {
            final IPath newTarget = new Path(source.getName());
            if (source.isDirectory()) {
                addDirectoryToProject(path, source, target.getFolder(newTarget), subMonitor);
            } else {
                addFileToProject(source, target.getFile(newTarget), subMonitor);
            }
        }
    }

    /**
     * Copy the supplied directory to the target project.
     *
     * Adapted from the generic sirius editor Palladio modeling project creation wizard:
     * {@link org.palladiosimulator.editors.sirius.ui.wizard.project.NewPalladioProjectWizard}.
     *
     * @param path
     *            The source directory path.
     * @param source
     *            The source directory file.
     * @param target
     *            The target directory file.
     * @param subMonitor
     *            The progress monitor to be updated.
     * @throws CoreException
     *             Thrown in case the directory could not be created or copying the files inside
     *             failed.
     */
    public static void addDirectoryToProject(final URI path, final File source, final IFolder target,
            final SubMonitor subMonitor) throws CoreException {
        if (!target.exists()) {
            target.create(IResource.NONE, true, null);
        }

        addToProject(path.appendSegment(source.getName()), target, subMonitor);
    }

    /**
     * Copy the supplied file to the target project.
     *
     * Adapted from the generic sirius editor Palladio modeling project creation wizard:
     * {@link org.palladiosimulator.editors.sirius.ui.wizard.project.NewPalladioProjectWizard}.
     *
     * @param source
     *            The source file.
     * @param target
     *            The target file.
     * @param subMonitor
     *            The progress monitor to be updated.
     * @throws CoreException
     *             Thrown if an error occurs during copying.
     */
    public static void addFileToProject(final File source, final IFile target, final SubMonitor subMonitor)
            throws CoreException {
        try (final InputStream contentStream = new FileInputStream(source)) {
            if (target.exists()) {
                target.setContents(contentStream, true, true, subMonitor);
            } else {
                target.create(contentStream, true, subMonitor);
            }
        } catch (final FileNotFoundException e) {
            // Handle exceptions by wrapping in IOException with useful error message
            throwCoreException("File " + source.getAbsolutePath() + " does not exist!");
        } catch (final IOException e) {
            throwCoreException("Cannot create input stream on file " + source.getAbsolutePath() + "! " + e.getMessage());
        }
    }

    /**
     * Throw a core exception based on a given error message.
     *
     * Adapted from the generic sirius editor Palladio modeling project creation wizard:
     * {@link org.palladiosimulator.editors.sirius.ui.wizard.project.NewPalladioProjectWizard}.
     *
     * @param message
     *            The message to present.
     * @throws CoreException
     *             Exception thrown by this utility method.
     */
    private static void throwCoreException(final String message) throws CoreException {
        final IStatus status = new Status(IStatus.ERROR, Config.PLUGIN_ID, IStatus.OK, message, null);
        throw new CoreException(status);
    }
}
