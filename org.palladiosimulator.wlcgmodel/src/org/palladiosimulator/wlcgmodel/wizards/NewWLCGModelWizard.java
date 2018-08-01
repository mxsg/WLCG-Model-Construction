package org.palladiosimulator.wlcgmodel.wizards;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.sirius.ui.tools.api.project.ModelingProjectManager;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.palladiosimulator.commons.eclipseutils.FileHelper;
import org.palladiosimulator.wlcgmodel.Config;
import org.palladiosimulator.wlcgmodel.BlueprintModelImport;

/**
 * A wizard to create a new calibrated WLCG model.
 *
 * This wizard is based on the generic PCM Project Wizard.
 */
public class NewWLCGModelWizard extends Wizard implements INewWizard {

    private WizardNewProjectCreationPage projectCreationPage;
    private IProject project;

    /**
     * Constructor for the WLCG Model Creation Wizard.
     */
    public NewWLCGModelWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    @Override
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
    }

    @Override
    public void addPages() {
        // Set the basic project page
        this.projectCreationPage = new WizardNewProjectCreationPage("NewPalladioProject");
        this.projectCreationPage.setDescription("Create a new WLCG Model Project.");
        this.projectCreationPage.setTitle("New WLCG Modeling Project");
        addPage(this.projectCreationPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
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

        return true;
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
     * @return The created project.
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

            URI modelParameterPath = URI.createURI(Config.MODEL_PARAMETER_FOLDER);

            // Get parameter description files

            String nodeDescriptionPath = modelParameterPath.appendSegment(Config.NODE_DESCRIPTION_FILENAME).toString();
            File nodeDescriptionFile = FileHelper.getFile(nodeDescriptionPath);

            String jobTypeDescriptionPath = modelParameterPath.appendSegment(Config.JOBS_DESCRIPTION_FILENAME)
                    .toString();
            File jobDescriptionFile = FileHelper.getFile(jobTypeDescriptionPath);

            // Import simulation model. This also copies it into our new project.
            BlueprintModelImport.importAndCompleteBlueprintModel(projectHandle, Config.MODEL_BLUEPRINT_URI,
                    nodeDescriptionFile, jobDescriptionFile);

            convertToModelingProject(projectHandle,
                    SubMonitor.convert(monitor, "Converting to Modeling Project", 2000));
        } finally {
            monitor.done();
        }
        return projectHandle;
    }

    /**
     * Create a new Eclipse project with the supplied project handle and project description.
     *
     * @param description
     *            The project description.
     * @param projectHandle
     *            The handle to the new project.
     * @param subMonitor
     *            The progress monitor to be updated.
     * @throws CoreException
     *             Thrown if the creation of the project fails.
     */
    private void createAndOpenProject(final IProjectDescription description, final IProject projectHandle,
            final SubMonitor subMonitor) throws CoreException {
        projectHandle.create(description, subMonitor.split(1000));
        if (subMonitor.isCanceled()) {
            throw new OperationCanceledException();
        }
        projectHandle.open(IResource.BACKGROUND_REFRESH, subMonitor.split(1000));
    }

    /**
     * Convert the project to a modeling project.
     *
     * @param projectHandle
     *            The handle of the project to be converted.
     * @param subMonitor
     *            The progress monitor to be updated.
     * @throws CoreException
     *             Thrown in case converting the project fails.
     */
    private void convertToModelingProject(final IProject projectHandle, final SubMonitor subMonitor)
            throws CoreException {
        ModelingProjectManager.INSTANCE.convertToModelingProject(projectHandle, subMonitor);
    }
}
