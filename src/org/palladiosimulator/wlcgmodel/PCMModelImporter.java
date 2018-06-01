package org.palladiosimulator.wlcgmodel;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;

public class PCMModelImporter {
	
	private static final String REPO_MODEL_FILENAME = "jobs.repository";
	
	public PCMModelImporter() {}
	
    public void completeAndImportModels(final URI blueprintPath) {
    	//final URI parameterPath, final IContainer target
    	
    	// For now, only import the repository model
    	URI repositoryPath = blueprintPath.appendSegment(REPO_MODEL_FILENAME);
    	
    	ResourceSet resourceSet = new ResourceSetImpl();
    	Resource repositoryResource = resourceSet.getResource(repositoryPath, true);
    	Repository repository = (Repository)repositoryResource.getContents().get(0);
    	
    	List<RepositoryComponent> components = repository.getComponents__Repository();
    	for(RepositoryComponent component : components) {
    		System.out.println("Component found: " + component.getId());
    	}
    	
//    	File repoModelSource = FileHelper.getFile(repositoryPath.toFileString());
//    	
//    	final ResourceSet resourceSet = new ResourceSetImpl();
//    	Resource resource = resourceSet.getResource(repositoryPath, true);
    	

        }
    
//    private void addToProject(final URI path, final IContainer target, final SubMonitor subMonitor)
//            throws CoreException {
//        for (final File source : FileHelper.getFiles(path.toString())) {
//            final IPath newTarget = new Path(source.getName());
//            if (source.isDirectory()) {
//                addFolderToProject(path, source, target.getFolder(newTarget), subMonitor);
//            } else {
//                addFileToProject(source, target.getFile(newTarget), subMonitor);
//            }
//        }
//    }

//    private void addFolderToProject(final URI path, final File source, final IFolder target,
//            final SubMonitor subMonitor) throws CoreException {
//        if (!target.exists()) {
//            target.create(IResource.NONE, true, null);
//        }
//
//        addToProject(path.appendSegment(source.getName()), target, subMonitor);
//    }

//    private void addFileToProject(final File source, final IFile target, final SubMonitor subMonitor)
//            throws CoreException {
//        try (final InputStream contentStream = new FileInputStream(source)) {
//            if (target.exists()) {
//                target.setContents(contentStream, true, true, subMonitor);
//            } else {
//                target.create(contentStream, true, subMonitor);
//            }
//        } catch (final FileNotFoundException e) {
//            throwCoreException("File " + source.getAbsolutePath() + " does not exist!");
//        } catch (final IOException e) {
//            throwCoreException(
//                    "Cannot create inpht stream on file " + source.getAbsolutePath() + "! " + e.getMessage());
//        } catch (final CoreException e) {
//            throwCoreException(e.getMessage());
//        }
//    }

}
