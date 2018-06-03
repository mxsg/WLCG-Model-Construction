package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.commons.eclipseutils.FileHelper;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.CompositionFactory;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.CompositeComponent;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;

public class PCMModelImporter {
	
	private static final String REPO_MODEL_FILENAME = "jobs.repository";
	private static final String NODE_DESCRIPTION_FILENAME = "nodes.json";
	private static final String JOB_DESCRIPTION_FILENAME = "jobs.json";
	
	private static final String COMPUTE_JOB_COMPOSITE_COMPONENT_ID = "WLCGBlueprint_computeJobCompositeComponent";
	private static final String BLUEPRINT_JOB_COMPONENT_ID = "WLCGBlueprint_blueprintJobComponent";
	private static final String BLUEPRINT_JOB_SEFF = "WLCGBlueprint_runBlueprintJobSEFF";
	private static final String BLUEPRINT_JOB_SEFF_INTERNAL_ACTION = "WLCGBlueprint_runBlueprintJobSEFF_internalAction";
	private static final String BLUEPRINT_JOB_SEFF_INTERNAL_CPU_RESOURCE_TYPE = "WLCGBlueprint_runBlueprintJobSEFF_cpu_resourcetype";


	
	public PCMModelImporter() {}
	
    public void completeModels(final URI modelsPath, final URI parameterPath) {
    	//final URI parameterPath, final IContainer target
    	
    	// For now, only import the repository model
    	URI repositoryPath = modelsPath.appendSegment(REPO_MODEL_FILENAME);
    	
    	ResourceSet resourceSet = new ResourceSetImpl();
    	Resource repositoryResource = resourceSet.getResource(repositoryPath, true);
    	Repository repository = (Repository)repositoryResource.getContents().get(0);
    	
    	// Find parameter files and import data
    	
    	String nodeDescriptionPath = parameterPath.appendSegment(NODE_DESCRIPTION_FILENAME).toString();
    	File nodeDescriptionFile = FileHelper.getFile(nodeDescriptionPath);
    	
    	List<NodeTypeDescription> nodes = new ArrayList<>();
    	try {
    		nodes = ParameterJSONImportHelper.readNodeTypes(nodeDescriptionFile);
    	}
    	catch(Exception e) {
    		System.out.println("Something went wrong when importing node types! E: " + e);
    	}
    	
    	String jobTypeDescriptionPath = parameterPath.appendSegment(JOB_DESCRIPTION_FILENAME).toString();
    	File jobDescriptionFile = FileHelper.getFile(jobTypeDescriptionPath);
    	List<JobTypeDescription> jobs = new ArrayList<>();
    	try {
    		jobs = ParameterJSONImportHelper.readJobTypes(jobDescriptionFile);
    	}
    	catch(Exception e) {
    		System.out.println("Something went wrong when importing jobs types! E: " + e);
    	}
    	
    	completeRepositoryModel(repository, jobs);
    	
    	try {
        	repositoryResource.save(null);
    	} catch(IOException e) {
    		System.out.println("Error while saving resources, e: " + e);
    	}
    	
//    	for(NodeTypeDescription node : nodes) {
//    		System.out.println(node);
//    	}
    	
    	
//    	File repoModelSource = FileHelper.getFile(repositoryPath.toFileString());
//    	
//    	final ResourceSet resourceSet = new ResourceSetImpl();
//    	Resource resource = resourceSet.getResource(repositoryPath, true);
    }
    
    public void completeModels(final URI modelsPath) {
    	System.out.println("Attempting to complete models ...");
    }
    
    public void completeRepositoryModel(Repository repository, List<JobTypeDescription> jobTypes) {
    	
    	List<RepositoryComponent> components = repository.getComponents__Repository();
    	for(RepositoryComponent component : components) {
    		System.out.println("Component found: " + component.getId());
    		System.out.println("Component with name: " + component.getEntityName());
    	}
    	
    	
    	// TODO include more checks for correct model structure
    	CompositeComponent computeJob = (CompositeComponent) findObjectWithId(components, COMPUTE_JOB_COMPOSITE_COMPONENT_ID);
    	
    	BasicComponent blueprintJob = (BasicComponent) findObjectWithId(components, BLUEPRINT_JOB_COMPONENT_ID);
//    	System.out.println("Found basic Component: " + EcoreUtil.getID(blueprintJob));
    	
    	
    	ServiceEffectSpecification seff = findObjectWithId(blueprintJob.getServiceEffectSpecifications__BasicComponent(),
    			BLUEPRINT_JOB_SEFF);
    	
    	// Todo Throw more meaningful exception to catch when calling this and notify user about invalid model
    	if(seff == null) {
    		throw new IllegalArgumentException("Invalid model blueprint!");
    	}
    	
    	ResourceDemandingSEFF resourceSeff = (ResourceDemandingSEFF) seff;
    	
//    	buildJobSEFF(resourceSeff, "test", jobTypes.get(0));
    	
    	for(JobTypeDescription job : jobTypes) {
    		System.out.println("Adding job type to repository: " + job.getTypeName());
    		buildAndAddJobComponentWithProvidedInterface(repository, job, resourceSeff, computeJob);
    	}
    	
//    	BasicComponent copyJob = EcoreUtil.copy(blueprintJob);
//    	System.out.println("ID of copy: " + EcoreUtil.getID(copyJob));
    }
    
    public BasicComponent buildAndAddJobComponentWithProvidedInterface(Repository repository, JobTypeDescription jobType, 
    		ResourceDemandingSEFF blueprintSeff, CompositeComponent computeJob) {
    	
    	BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
    	
    	String jobTypeName = jobType.getTypeName();
    	
    	component.setEntityName(jobTypeName);
    	repository.getComponents__Repository().add(component);
    	
    	// Create the interface with a single signature
    	OperationInterface typeInterface = RepositoryFactory.eINSTANCE.createOperationInterface();
    	typeInterface.setEntityName("interface_" + jobTypeName);
    	
    	repository.getInterfaces__Repository().add(typeInterface);
    	
    	// Create a signature for the interface
    	OperationSignature jobInterfaceSignature = RepositoryFactory.eINSTANCE.createOperationSignature();
    	jobInterfaceSignature.setEntityName("run_" + jobTypeName);
    	
    	typeInterface.getSignatures__OperationInterface().add(jobInterfaceSignature);
    	
    	// Set the interface for the job
    	OperationProvidedRole opProvidedRole = RepositoryFactory.eINSTANCE.createOperationProvidedRole();
    	opProvidedRole.setEntityName("provided_role_" + jobTypeName);
    	
    	// Set the interface for the role
    	opProvidedRole.setProvidedInterface__OperationProvidedRole(typeInterface);
    	
    	// Add the provided role to the component
    	component.getProvidedRoles_InterfaceProvidingEntity().add(opProvidedRole);
    	
    	ResourceDemandingSEFF seff = buildJobSEFF(blueprintSeff, jobTypeName, jobType);
    	seff.setDescribedService__SEFF(jobInterfaceSignature);
    	
    	// Add SEFF to component
    	component.getServiceEffectSpecifications__BasicComponent().add(seff);
    	
    	// Add component to repository
    	repository.getComponents__Repository().add(component);
    	
    	// Add component to computing job component
    	AssemblyContext assembly = CompositionFactory.eINSTANCE.createAssemblyContext();
    	assembly.setEncapsulatedComponent__AssemblyContext(component);
    	assembly.setParentStructure__AssemblyContext(computeJob);
    	
    	// Provided Role for the Composite Component (Computing Job)
    	OperationProvidedRole compositeRole = RepositoryFactory.eINSTANCE.createOperationProvidedRole();
    	compositeRole.setProvidedInterface__OperationProvidedRole(typeInterface);
    	compositeRole.setEntityName("run_" + jobTypeName);
    	computeJob.getProvidedRoles_InterfaceProvidingEntity().add(compositeRole);
    	
    	ProvidedDelegationConnector connector = CompositionFactory.eINSTANCE.createProvidedDelegationConnector();
    	connector.setAssemblyContext_ProvidedDelegationConnector(assembly);
    	// opProvidedRole is the role associated with the basic compute job (implementing the interface)
    	connector.setInnerProvidedRole_ProvidedDelegationConnector(opProvidedRole);
    	connector.setOuterProvidedRole_ProvidedDelegationConnector(compositeRole);
    	
    	connector.setParentStructure__Connector(computeJob);
    	
    	return component;
    }
    
    public ResourceDemandingSEFF buildJobSEFF(ResourceDemandingSEFF blueprintSEFF, String seffName, JobTypeDescription jobType) {
    	
//    	ResourceDemandingSEFF newSeff = EcoreUtil.copy(blueprintSEFF);
//    	
//    	// Change IDs for the contained elements
//    	EcoreUtil.setID(newSeff, EcoreUtil.generateUUID());
//    	
//    	// Insert correct resource demand into SEFF
    	
//    	ProcessingResourceType cpuResource = null;
//    	
//    	TreeIterator<EObject> i = blueprintSEFF.eAllContents();
//    	while(i.hasNext()) {
//    		EObject obj = i.next();
//    		if(obj instanceof ProcessingResourceType) {
//    			cpuResource = (ProcessingResourceType) obj;
//    		}
//    	}
    	
    	
    	// Todo Remove this
    	
    	ResourceDemandingSEFF newSeff = EcoreUtil.copy(blueprintSEFF);
    	EcoreUtil.setID(newSeff, EcoreUtil.generateUUID());
    	    	
    	InternalAction cpuDemandAction = null;
    	PCMRandomVariable cpuDemandVariable = null;
    	
    	// Iterate through all the contents of the Seff to change IDs and find elements to change
    	TreeIterator<EObject> j = newSeff.eAllContents();
    	while(j.hasNext()) {
    		EObject obj = j.next();
    		
    		// Reset all IDs for contained objects that have IDs
    		// Todo What is the clean way to do this?
    		try {
    			EcoreUtil.setID(obj, EcoreUtil.generateUUID());
    		} catch(IllegalArgumentException e) {
    			// Object does not have ID, do not reset
    		}
    		
    		if(obj instanceof InternalAction) {
    			cpuDemandAction = (InternalAction) obj;
    			
    		} else if(obj instanceof PCMRandomVariable) {
    			cpuDemandVariable = (PCMRandomVariable) obj;
    		}
    	}
    	    	
    	cpuDemandVariable.setSpecification(jobType.getCpuDemandStoEx());
    	
    	return newSeff;
    	    	
//    	// Add a SEFF for the component
//    	ResourceDemandingSEFF seff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
//    	
//    	AbstractAction startAction = SeffFactory.eINSTANCE.createStartAction();
//    	AbstractAction stopAction = SeffFactory.eINSTANCE.createStopAction();
//    	
//    	InternalAction computeAction = SeffFactory.eINSTANCE.createInternalAction();
//    	
//    	ParametricResourceDemand cpuDemand = SeffPerformanceFactory.eINSTANCE.createParametricResourceDemand();
//    	
//    	// Set parameters for resource demand
//    	PCMRandomVariable demandVariable = CoreFactory.eINSTANCE.createPCMRandomVariable();
//    	demandVariable.setSpecification(jobType.getCpuDemandStoEx());
//    	
//    	cpuDemand.setSpecification_ParametericResourceDemand(demandVariable);
//    	cpuDemand.setRequiredResource_ParametricResourceDemand(cpuResource);
//    	
////    	computeAction.setResourceDemandingBehaviour_AbstractAction(value);
//    	
////    	computeAction.setResourceDemandingBehaviour_AbstractAction(cpuDemand);
//    	
//    	// Construct correct action order
//    	startAction.setSuccessor_AbstractAction(computeAction);
//    	computeAction.setPredecessor_AbstractAction(startAction);
//    	
//    	computeAction.setSuccessor_AbstractAction(stopAction);
//    	stopAction.setPredecessor_AbstractAction(computeAction);
//    	
////    	cpuDemand.setRequiredResource_ParametricResourceDemand(value);
    }
    
    
    
    public void doBuildBasicComponentWithProvidedInterface(Resource res) {
    	Repository repository = RepositoryFactory.eINSTANCE.createRepository();
    	repository.setEntityName("Repository containing other elements");
    	// add root (repository) to resource:
    	res.getContents().add(repository);
    	
    	BasicComponent bc = RepositoryFactory.eINSTANCE.createBasicComponent();
    	bc.setEntityName("My Basic Component");
    	repository.getComponents__Repository().add(bc);  //add first class entity
    	
    	OperationInterface myInterface = RepositoryFactory.eINSTANCE.createOperationInterface();
    	myInterface.setEntityName("My Interface");
    	repository.getInterfaces__Repository().add(myInterface); //add first class entity
    	
    	OperationProvidedRole opProvRole = RepositoryFactory.eINSTANCE.createOperationProvidedRole();
    	opProvRole.setEntityName("Provided Role of Basic Component");
    	
    	// set the interface for the role:
    	opProvRole.setProvidedInterface__OperationProvidedRole(myInterface);
    	
    	// set/add the role for basic component:
    	bc.getProvidedRoles_InterfaceProvidingEntity().add(opProvRole);
    }	

    
    
    /**
     * Find the object with known ID in the list, return null if there is no such object.
     * 
     * @param objects
     * @param id
     */
    private <T extends EObject> T findObjectWithId(List<T> objects, String id) {
    	try {
    		return objects.stream().filter(obj -> EcoreUtil.getID(obj).contentEquals(id)).findAny().get();
    	} catch(NoSuchElementException e) {
    		// Could not find matching element
    		return null;
    	}
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
