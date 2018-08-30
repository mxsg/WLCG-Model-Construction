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
import org.palladiosimulator.wlcgmodel.Config;
import org.palladiosimulator.wlcgmodel.BlueprintModelImport;

/**
 * Handle a context click and extract the project and file that has been clicked.
 *
 * This class is adapted from
 * https://stackoverflow.com/questions/6892294/eclipse-plugin-how-to-get-the-path-to-the-currently-selected-project
 *
 * @author Maximilian Stemmer-Grabow
 */
public class NavigationPopupHandler extends AbstractHandler {

    private IWorkbenchWindow window;
    private IWorkbenchPage activePage;

    private IProject selectedProject;
    private IResource selectedResource;

    @SuppressWarnings("unused")
    private IFile selectedFile;
    private IFolder selectedFolder;

    /**
     * Construct a new popup menu item handler.
     */
    public NavigationPopupHandler() {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Get the project and file name from the initiating event if at all possible
        if (!extractSelection(event)) {
            return null;
        }

        if (selectedFolder == null || !selectedFolder.exists()) {
            MessageDialog.openError(this.window.getShell(), "Invalid Selection",
                    "Please select a folder containing parameter files for the model completion.");
            return null;
        }

        IFile iNodeDescription = selectedFolder.getFile(Config.NODE_DESCRIPTION_FILENAME);
        IFile iJobDescription = selectedFolder.getFile(Config.JOBS_DESCRIPTION_FILENAME);

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

        boolean modelCompletionSuccess = false;
        try {
            modelCompletionSuccess = BlueprintModelImport.importAndCompleteBlueprintModel(selectedProject, Config.MODEL_BLUEPRINT_URI,
                    nodeDescriptionFile, jobDescriptionFile);
        } catch (Exception e) {
            // TODO Show more meaningful error here
            showError("Error while completing the model:\n\n" + e.toString() + "\n" + e.getMessage());
            return null;
        }
        
        if (modelCompletionSuccess) {
            MessageDialog.openInformation(this.window.getShell(), "Model Completion Successful",
                    "All models were successfully completed.");
            return null;
        } else {
        	showError("An error occurred while completing the model!");
            return null;
        }
    }

    /**
     * Extract the selected resource and its root project from the navigation popup execution event.
     *
     * @param event
     *            The initiating selection event.
     * @return True, if the selection could be successfully extracted, false otherwise.
     */
    private boolean extractSelection(ExecutionEvent event) {

        // Get the active workbench page and the selection in it
        this.window = HandlerUtil.getActiveWorkbenchWindow(event);
        this.activePage = this.window.getActivePage();
        ISelection selection = this.activePage.getSelection();

        if (selection instanceof ITreeSelection) {
            TreeSelection treeSelection = (TreeSelection) selection;
            TreePath[] treePaths = treeSelection.getPaths();

            if (treePaths.length == 0) {
                showError("Nothing selected. Please select a folder with model parameters.");
                return false;
            }
            TreePath treePath = treePaths[0];

            // The first segment should be an IProject
            Object firstSegmentObj = treePath.getFirstSegment();
            this.selectedProject = ((IAdaptable) firstSegmentObj).getAdapter(IProject.class);
            if (this.selectedProject == null) {
                showError("Invalid selection. Could not retrieve selected project.");
                return false;
            }

            // The last segment should be an IResource
            Object lastSegmentObj = treePath.getLastSegment();
            this.selectedResource = ((IAdaptable) lastSegmentObj).getAdapter(IResource.class);
            if (this.selectedResource == null) {
                showError("Invalid selection. Could not retrieve selected item.");
                return false;
            }

            // Get file and folder reference from selected resource
            this.selectedFile = ((IAdaptable) lastSegmentObj).getAdapter(IFile.class);
            this.selectedFolder = ((IAdaptable) lastSegmentObj).getAdapter(IFolder.class);

            return true;

        } else {
            showError("Selection error. Unexpected selection class.");

            return false;
        }
    }

    /**
     * Convenience method to check whether the supplied file exists. Shows an error dialog in case
     * the check fails.
     *
     * @param file
     *            The file to check for existence.
     * @return True if the file exists, else false.
     */
    private boolean checkFileExists(IFile file) {

        if (!file.exists()) {
            showError("No file with name " + file.getName() + " could be found, but is required.");
            return false;
        }
        return true;
    }

    /**
     * Show an error popup with the supplied error message.
     *
     * @param message
     *            The message to show.
     */
    private void showError(String message) {
        if (message == null || message.length() == 0) {
            message = "An error occured.";
        }
        MessageDialog.openError(this.window.getShell(), "Model Completion Error", message);
    }
}
