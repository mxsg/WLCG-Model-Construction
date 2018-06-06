package org.palladiosimulator.wlcgmodel.commands;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.palladiosimulator.wlcgmodel.PCMModelImporter;

/**
 * Handle a context click and extract the project and file that has been
 * clicked. This class is adapted from
 * https://stackoverflow.com/questions/6892294/eclipse-plugin-how-to-get-the-path-to-the-currently-selected-project.
 *
 */
public class NavigationPopupHandler extends AbstractHandler {

    private static final String NODE_DESCRIPTION_FILENAME = "nodes.json";
    private static final String JOBS_DESCRIPTION_FILENAME = "jobs.json";

    private IWorkbenchWindow window;
    private IWorkbenchPage activePage;

    private IProject selectedProject;
    private IResource selectedResource;
    private IFile selectedFile;
    private IFolder selectedFolder;

    private String workspaceName;
    private String projectName;
    private String fileName;

    public NavigationPopupHandler() {
        // Empty constructor
    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Get the project and file name from the initiating event if at all possible
        if (!extractProjectAndFileFromInitiatingEvent(event)) {
            return null;
        }

        if (selectedFolder == null || !selectedFolder.exists()) {
            MessageDialog.openError(this.window.getShell(), "Invalid Selection",
                    "Please select a folder containing parameter files for the model completion.");
            return null;
        }

        IFile iNodeDescription = selectedFolder.getFile(NODE_DESCRIPTION_FILENAME);
        IFile iJobDescription = selectedFolder.getFile(JOBS_DESCRIPTION_FILENAME);

        if (!checkFileExists(iNodeDescription) || !checkFileExists(iJobDescription)) {
            // This already shows an error if a file is not found, so simply bail.
            return null;
        }

        IPath nodePath = iNodeDescription.getLocation();
        IPath jobPath = iJobDescription.getLocation();

        if (nodePath == null || jobPath == null) {
            showError("Could not construct paths to parameter files.");
            return null;
        }

        File nodeDescriptionFile = nodePath.toFile();
        File jobDescriptionFile = jobPath.toFile();

        boolean userConfirmedOverwrite = MessageDialog.openQuestion(this.window.getShell(), "Overwrite model files?",
                "This operation overwrites all conflicting model files in this project. Proceed?");
        if (!userConfirmedOverwrite) {
            // User aborted operation, do not try to complete models
            return null;
        }

        try {
            PCMModelImporter.importAndCompleteBlueprintModel(selectedProject, nodeDescriptionFile, jobDescriptionFile);
        } catch (Exception e) {
            // TODO Show more meaningful error here
            showError("Error while completing the model:\n\n" + e.toString() + "\n" + e.getMessage());
            return null;
        }

        MessageDialog.openInformation(this.window.getShell(), "Model Completion Successful",
                "All models were successfully completed.");
        return null;
    }

    private boolean checkFileExists(IFile file) {

        if (!file.exists()) {
            showError("No file with name " + file.getName() + " could be found, but is required.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        if (message == null || message.length() == 0) {
            message = "An error occured.";
        }
        MessageDialog.openError(this.window.getShell(), "Model Completion Error", message);
    }

    // TODO Catch index out of bounds exception when user did not select anything
    private boolean extractProjectAndFileFromInitiatingEvent(ExecutionEvent event) {
        this.window = HandlerUtil.getActiveWorkbenchWindow(event);
        // Get the active WorkbenchPage
        this.activePage = this.window.getActivePage();

        // Get the Selection from the active WorkbenchPage page
        ISelection selection = this.activePage.getSelection();
        if (selection instanceof ITreeSelection) {
            TreeSelection treeSelection = (TreeSelection) selection;
            TreePath[] treePaths = treeSelection.getPaths();

            if (treePaths.length == 0) {
                MessageDialog.openError(this.window.getShell(), "Nothing selected",
                        "Please select a folder with model parameters.");
                return false;
            }
            TreePath treePath = treePaths[0];

            // The TreePath contains a series of segments in our usage:
            // o The first segment is usually a project
            // o The last segment generally refers to the file

            // The first segment should be a IProject
            Object firstSegmentObj = treePath.getFirstSegment();
            this.selectedProject = (IProject) ((IAdaptable) firstSegmentObj).getAdapter(IProject.class);
            if (this.selectedProject == null) {
                MessageDialog.openError(this.window.getShell(), "Navigator Popup", getClassHierarchyAsMsg(
                        "Expected the first segment to be IAdapatable to an IProject.\nBut got the following class hierarchy instead.",
                        "Make sure to directly select a file.", firstSegmentObj));
                return false;
            }

            // The last segment should be an IResource
            Object lastSegmentObj = treePath.getLastSegment();
            this.selectedResource = (IResource) ((IAdaptable) lastSegmentObj).getAdapter(IResource.class);
            if (this.selectedResource == null) {
                MessageDialog.openError(this.window.getShell(), "Navigator Popup", getClassHierarchyAsMsg(
                        "Expected the last segment to be IAdapatable to an IResource.\nBut got the following class hierarchy instead.",
                        "Make sure to directly select a file.", firstSegmentObj));
                return false;
            }

            // TODO Clean up this code

            // As the last segment is an IResource we should be able to get an IFile
            // reference from it
            this.selectedFile = (IFile) ((IAdaptable) lastSegmentObj).getAdapter(IFile.class);
            this.selectedFolder = (IFolder) ((IAdaptable) lastSegmentObj).getAdapter(IFolder.class);

            // Extract additional information from the IResource and IProject
            this.workspaceName = this.selectedResource.getWorkspace().getRoot().getLocation().toOSString();
            this.projectName = this.selectedProject.getName();
            this.fileName = this.selectedResource.getName();

            return true;
        } else {
            String selectionClass = selection.getClass().getSimpleName();
            MessageDialog.openError(this.window.getShell(), "Unexpected Selection Class", String
                    .format("Expected a TreeSelection but got a %s instead.\nProcessing Terminated.", selectionClass));
        }

        return false;
    }

    @SuppressWarnings("rawtypes")
    private static String getClassHierarchyAsMsg(String msgHeader, String msgTrailer, Object theObj) {
        String msg = msgHeader + "\n\n";

        Class theClass = theObj.getClass();
        while (theClass != null) {
            msg = msg + String.format("Class=%s\n", theClass.getName());
            Class[] interfaces = theClass.getInterfaces();
            for (Class theInterface : interfaces) {
                msg = msg + String.format("    Interface=%s\n", theInterface.getName());
            }
            theClass = theClass.getSuperclass();
        }

        msg = msg + "\n" + msgTrailer;

        return msg;
    }
}
