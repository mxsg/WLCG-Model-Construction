package org.palladiosimulator.wlcgmodel.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.sirius.ui.tools.api.project.ModelingProjectManager;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.palladiosimulator.commons.eclipseutils.FileHelper;
import org.palladiosimulator.wlcgmodel.Config;
import org.palladiosimulator.wlcgmodel.PCMModelCompletion;

/**
 * A wizard to create a new calibrated WLCG model.
 *
 * This wizard is based on the generic PCM Project Wizard.
 */
public class NewWLCGModelWizard extends Wizard implements INewWizard {

    /** ID of the perspective to be opened after project creation. */
    private static final String PERSPECTIVE_ID = "org.palladiosimulator.pcmbench.perspectives.palladio";

    /** Path to the directory containing the blueprint model files. */
    private static final String MODEL_PARAMETER_FOLDER = "platform:/plugin/org.palladiosimulator.wlcgmodel/parameters";

    private WizardNewProjectCreationPage projectCreationPage;
    private IProject project;
    private IConfigurationElement config;
    private IWorkbench workbench;

    /**
     * Constructor for the WLCG Model Creation Wizard.
     */
    public NewWLCGModelWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        this.workbench = workbench;
    }

    @Override
    public void addPages() {
        // set the basic project page
        this.projectCreationPage = new WizardNewProjectCreationPage("NewPalladioProject");
        this.projectCreationPage.setDescription("Create a new WLCG Model Project.");
        this.projectCreationPage.setTitle("New WLCG Modeling Project");
        addPage(this.projectCreationPage);
    }

    @Override
    public boolean performFinish() {
        final IProject projectHandle = this.projectCreationPage.getProjectHandle();

        final java.net.URI projectURI = (!this.projectCreationPage.useDefaults())
                ? this.projectCreationPage.getLocationURI()
                : null;

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();

        final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());
        desc.setLocationURI(projectURI);

        // Project is created inside of a workspace operation
        final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {

            @Override
            protected void execute(final IProgressMonitor monitor) throws CoreException {
                NewWLCGModelWizard.this.project = createProject(desc, projectHandle, monitor);
            }
        };

        try {
            getContainer().run(true, true, op);
        } catch (final Exception e) {
            MessageDialog.openError(getShell(), "Error", "An unexpected error occured. See stack trace");
            e.printStackTrace();
            return false;
        }

        if (this.project == null) {
            return false;
        }

        BasicNewProjectResourceWizard.updatePerspective(this.config);
        BasicNewProjectResourceWizard.selectAndReveal(this.project, this.workbench.getActiveWorkbenchWindow());

        if (!getCurrentPerspectiveId().equals(PERSPECTIVE_ID)) {
            boolean confirm = MessageDialog.openConfirm(getShell(), "Palladio Perspective",
                    "This project is associated with the Palladio perspective.\n\nDo you want to open this perspective now?");
            if (confirm) {
                openPalladioPerspective();
            }
        }

        return true;
    }

    private void openPalladioPerspective() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        try {
            workbench.showPerspective(PERSPECTIVE_ID, window);
        } catch (WorkbenchException e) {
            MessageDialog.openError(getShell(), "Error", "Could not open Palladio Perspective");
            e.printStackTrace();
        }
    }

    private String getCurrentPerspectiveId() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IWorkbenchPage page = window.getActivePage();
        IPerspectiveDescriptor perspective = page.getPerspective();
        return perspective.getId();
    }

    /**
     * This creates the project in the workspace.
     *
     * @param description
     *            The description to set for the project.
     * @param projectHandle
     *            The handle to the project.
     * @param monitor
     *            The progress monitor to be updated.
     * @throws CoreException
     *             Thrown if model files could not be copied to project.
     * @throws OperationCanceledException
     *             Thrown in case the user cancels the project creation.
     */
    private IProject createProject(final IProjectDescription description, final IProject projectHandle,
            final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
        try {
            monitor.beginTask("Creating Project", 8000);
            createAndOpenProject(description, projectHandle, SubMonitor.convert(monitor, "Main Task", 2000));

            // Copy blueprint model files to newly created project
            copyModelsToProject(computeBlueprintPath(), projectHandle,
                    SubMonitor.convert(monitor, "Creating model files", 2000));

            /* Construct models and load into project */
            PCMModelCompletion importer = new PCMModelCompletion();

            // Compute project location
            URI projectURI = URI.createURI(projectHandle.getFullPath().toString());

            importer.loadParametersAndCompleteModels(projectURI, computeParameterPath());

            convertToModelingProject(projectHandle,
                    SubMonitor.convert(monitor, "Converting to Modeling Project", 2000));
        } finally {
            monitor.done();
        }
        return projectHandle;
    }

    private void createAndOpenProject(final IProjectDescription description, final IProject projectHandle,
            final SubMonitor subMonitor) throws CoreException {
        projectHandle.create(description, subMonitor.split(1000));
        if (subMonitor.isCanceled()) {
            throw new OperationCanceledException();
        }
        projectHandle.open(IResource.BACKGROUND_REFRESH, subMonitor.split(1000));
    }

    private void copyModelsToProject(final URI path, final IContainer target, final SubMonitor subMonitor)
            throws CoreException {
        addToProject(path, target, subMonitor);
    }

    private void addToProject(final URI path, final IContainer target, final SubMonitor subMonitor)
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

    private void addFolderToProject(final URI path, final File source, final IFolder target,
            final SubMonitor subMonitor) throws CoreException {
        if (!target.exists()) {
            target.create(IResource.NONE, true, null);
        }

        addToProject(path.appendSegment(source.getName()), target, subMonitor);
    }

    private void addFileToProject(final File source, final IFile target, final SubMonitor subMonitor)
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

    private static URI computeParameterPath() {
        return URI.createURI(MODEL_PARAMETER_FOLDER);
    }

    /**
     * Throw a core exception based on a given error message.
     *
     * @param message
     *            The message to present.
     * @throws CoreException
     *             The exception to throw.
     */
    private void throwCoreException(final String message) throws CoreException {
        final IStatus status = new Status(IStatus.ERROR, "org.palladiosimulator.editors.sirius.custom.wizard",
                IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
     * Convert the project to a modeling project.
     */
    private void convertToModelingProject(final IProject projectHandle, final SubMonitor subMonitor)
            throws CoreException {
        ModelingProjectManager.INSTANCE.convertToModelingProject(projectHandle, subMonitor);
    }
}
