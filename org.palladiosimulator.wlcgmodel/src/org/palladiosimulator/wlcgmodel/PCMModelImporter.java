package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.commons.eclipseutils.FileHelper;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.allocation.AllocationFactory;
import org.palladiosimulator.pcm.core.CoreFactory;
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
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.OpenWorkload;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;

public class PCMModelImporter {

    private static final String REPO_MODEL_FILENAME = "jobs.repository";
    private static final String SYSTEM_MODEL_FILENAME = "jobs.system";
    private static final String RESOURCE_ENVIRONMENT_MODEL_FILENAME = "nodes.resourceenvironment";
    private static final String ALLOCATION_MODEL_FILENAME = "newAllocation.allocation";
    private static final String USAGE_MODEL_FILENAME = "wlcg.usagemodel";

    private static final String NODE_DESCRIPTION_FILENAME = "nodes.json";
    private static final String JOB_DESCRIPTION_FILENAME = "jobs.json";

    private static final String COMPUTE_JOB_COMPOSITE_COMPONENT_ID = "WLCGBlueprint_computeJobCompositeComponent";
    private static final String BLUEPRINT_JOB_COMPONENT_ID = "WLCGBlueprint_blueprintJobComponent";
    private static final String BLUEPRINT_JOB_SEFF = "WLCGBlueprint_runBlueprintJobSEFF";
    private static final String BLUEPRINT_JOB_SEFF_INTERNAL_ACTION = "WLCGBlueprint_runBlueprintJobSEFF_internalAction";
    private static final String BLUEPRINT_JOB_SEFF_INTERNAL_CPU_RESOURCE_TYPE = "WLCGBlueprint_runBlueprintJobSEFF_cpu_resourcetype";
    private static final String COMPUTE_ASSEMBLY_CONTEXT_SYSTEM = "WLCGBlueprint_computeJobAssemblyContextSystem";
    private static final String BLUEPRINT_NODE = "WLCGBlueprint_blueprintNode";
    private static final String BLUEPRINT_CPU = "WLCGBlueprint_blueprintCPU";
    private static final String BLUEPRINT_HDD = "WLCGBlueprint_blueprintHDD";

    private static final String BLUEPRINT_USAGE_SCENARIO = "WLCGBlueprint_blueprintUsageScenario";
    private static final String BLUEPRINT_ENTRY_LEVEL_SYSTEM_CALL = "WLCGBlueprint_blueprintEntryLevelSystemCall";

    private Map<String, OperationInterface> jobInterfaces = new HashMap<>();
    private Map<String, OperationProvidedRole> providedRolesComputeJobAssembly = new HashMap<>();
    private Map<String, OperationProvidedRole> providedRolesSystem = new HashMap<>();
    private Map<String, OperationSignature> jobSignatures = new HashMap<>();

    private List<ResourceContainer> resourceContainerTypes = new ArrayList<>();
    private AssemblyContext computeJobAssembly = null;

    public PCMModelImporter() {
    }

    public void completeModels(final URI modelsPath, final URI parameterPath) {
        // final URI parameterPath, final IContainer target

        // For now, only import the repository model
        URI repositoryPath = modelsPath.appendSegment(REPO_MODEL_FILENAME);

        ResourceSet resourceSet = new ResourceSetImpl();
        Resource repositoryResource = resourceSet.getResource(repositoryPath, true);
        Repository repository = (Repository) repositoryResource.getContents().get(0);

        // Find parameter files and import data

        String nodeDescriptionPath = parameterPath.appendSegment(NODE_DESCRIPTION_FILENAME).toString();
        File nodeDescriptionFile = FileHelper.getFile(nodeDescriptionPath);

        List<NodeTypeDescription> nodes = new ArrayList<>();
        try {
            nodes = ParameterJSONImportHelper.readNodeTypes(nodeDescriptionFile);
        } catch (Exception e) {
            System.out.println("Something went wrong when importing node types! E: " + e);
        }

        String jobTypeDescriptionPath = parameterPath.appendSegment(JOB_DESCRIPTION_FILENAME).toString();
        File jobDescriptionFile = FileHelper.getFile(jobTypeDescriptionPath);
        List<JobTypeDescription> jobs = new ArrayList<>();
        try {
            jobs = ParameterJSONImportHelper.readJobTypes(jobDescriptionFile);
        } catch (Exception e) {
            System.out.println("Something went wrong when importing jobs types! E: " + e);
        }

        completeRepositoryModel(repository, jobs);

        // Complete system model

        Resource systemResource = resourceSet.getResource(modelsPath.appendSegment(SYSTEM_MODEL_FILENAME), true);
        org.palladiosimulator.pcm.system.System system = (org.palladiosimulator.pcm.system.System) systemResource
                .getContents().get(0);

        List<AssemblyContext> systemAssemblies = system.getAssemblyContexts__ComposedStructure();
        AssemblyContext computeAssembly = (AssemblyContext) findObjectWithId(systemAssemblies,
                COMPUTE_ASSEMBLY_CONTEXT_SYSTEM);

        // Add for later use
        this.computeJobAssembly = computeAssembly;

        for (JobTypeDescription jobDescription : jobs) {
            String jobTypeName = jobDescription.getTypeName();

            OperationInterface jobInterface = jobInterfaces.get(jobTypeName);
            OperationProvidedRole assemblyProvidedRole = providedRolesComputeJobAssembly.get(jobTypeName);

            // Create new system provided role and add to system model
            OperationProvidedRole role = RepositoryFactory.eINSTANCE.createOperationProvidedRole();
            role.setEntityName(jobTypeName);
            role.setProvidedInterface__OperationProvidedRole(jobInterface);

            this.providedRolesSystem.put(jobTypeName, role);
            role.setProvidingEntity_ProvidedRole(system);

            ProvidedDelegationConnector connector = CompositionFactory.eINSTANCE.createProvidedDelegationConnector();
            connector.setOuterProvidedRole_ProvidedDelegationConnector(role);
            connector.setInnerProvidedRole_ProvidedDelegationConnector(assemblyProvidedRole);
            connector.setAssemblyContext_ProvidedDelegationConnector(computeAssembly);

            // Add connector to system model
            connector.setParentStructure__Connector(system);
        }

        // Complete Resource Environment

        Resource resourceEnvironmentResource = resourceSet
                .getResource(modelsPath.appendSegment(RESOURCE_ENVIRONMENT_MODEL_FILENAME), true);
        ResourceEnvironment resEnv = (ResourceEnvironment) resourceEnvironmentResource.getContents().get(0);

        List<ResourceContainer> resourceContainers = resEnv.getResourceContainer_ResourceEnvironment();
        ResourceContainer blueprintContainer = findObjectWithId(resourceContainers, BLUEPRINT_NODE);

        List<Stereotype> blueprintStereotypes = StereotypeAPI.getAppliedStereotypes(blueprintContainer);

        Stereotype loadBalancedResourceContainerStereotype = blueprintStereotypes.stream()
                .filter(stereotype -> "StaticLoadbalancedResourceContainer".equals(stereotype.getName())).findAny()
                .orElse(null);

        if (loadBalancedResourceContainerStereotype == null) {
            throw new IllegalArgumentException("Invalid model blueprint!");
        }

        // System.out.println("Found stereotypes on blueprint element:");
        // for(Stereotype stereotype : blueprintStereotypes) {
        // System.out.println(stereotype);
        // }

        if (blueprintContainer == null) {
            throw new IllegalArgumentException("Invalid resource environment model");
        }

        for (NodeTypeDescription nodeType : nodes) {
            String nodeTypeName = nodeType.getName();

            ResourceContainer newNode = EcoreUtil.copy(blueprintContainer);

            newNode.setEntityName(nodeTypeName);

            List<ProcessingResourceSpecification> resourceSpecs = newNode
                    .getActiveResourceSpecifications_ResourceContainer();

            ProcessingResourceSpecification cpuResourceSpec = findObjectWithId(resourceSpecs, BLUEPRINT_CPU);

            // Set CPU properties
            cpuResourceSpec.setNumberOfReplicas(nodeType.getCores());

            PCMRandomVariable processingRate = CoreFactory.eINSTANCE.createPCMRandomVariable();
            processingRate.setSpecification(String.valueOf(nodeType.getComputingRate()));

            cpuResourceSpec.setProcessingRate_ProcessingResourceSpecification(processingRate);

            // Do not forget to change all IDs for the copied objects
            changeIds(newNode);

            // Add new resource container to tracked resource containers
            this.resourceContainerTypes.add(newNode);

            // Add new node to resource environment
            newNode.setResourceEnvironment_ResourceContainer(resEnv);

            // Add stereotypes for Architectural Template Application
            // This needs to be done last so that the new node is already included in a
            // resource
            // TODO Put this elsewhere, move all AT application code together!
            StereotypeAPI.applyStereotype(newNode, loadBalancedResourceContainerStereotype);

            // Set duplication number to correct value
            StereotypeAPI.setTaggedValue(newNode, nodeType.getNodeCount(), "StaticLoadbalancedResourceContainer",
                    "numberOfReplicas");
        }

        // Complete Usage Model

        Resource usageModelResource = resourceSet.getResource(modelsPath.appendSegment(USAGE_MODEL_FILENAME), true);
        UsageModel usageModel = (UsageModel) usageModelResource.getContents().get(0);

        List<UsageScenario> usageScenarios = usageModel.getUsageScenario_UsageModel();
        UsageScenario blueprintUsageScenario = findObjectWithId(usageScenarios, BLUEPRINT_USAGE_SCENARIO);

        for (JobTypeDescription jobType : jobs) {
            String jobTypeName = jobType.getTypeName();

            UsageScenario newUsageScenario = EcoreUtil.copy(blueprintUsageScenario);
            newUsageScenario.setEntityName("usage_scenario_" + jobTypeName);

            EObject obj = findObjectWithIdRecursively(newUsageScenario, BLUEPRINT_ENTRY_LEVEL_SYSTEM_CALL);
            EntryLevelSystemCall systemCall = null;
            if (obj instanceof EntryLevelSystemCall) {
                systemCall = (EntryLevelSystemCall) obj;
            }

            systemCall.setOperationSignature__EntryLevelSystemCall(jobSignatures.get(jobTypeName));
            systemCall.setProvidedRole_EntryLevelSystemCall(providedRolesSystem.get(jobTypeName));

            OpenWorkload workload = (OpenWorkload) newUsageScenario.getWorkload_UsageScenario();
            PCMRandomVariable interarrivalTime = workload.getInterArrivalTime_OpenWorkload();
            interarrivalTime.setSpecification(jobType.getInterarrivalStoEx());

            // Change all IDs in new usage scenario structure
            this.changeIds(newUsageScenario);

            newUsageScenario.setUsageModel_UsageScenario(usageModel);
        }

        // Remove blueprint usage scenario from the usage model
        // This is important to not alter the generated load profile.
        blueprintUsageScenario.setUsageModel_UsageScenario(null);

        // Complete Allocation Model

        Resource allocationModelResource = resourceSet.getResource(modelsPath.appendSegment(ALLOCATION_MODEL_FILENAME),
                true);
        Allocation allocation = (Allocation) allocationModelResource.getContents().get(0);

        for (ResourceContainer nodeType : resourceContainerTypes) {

            AllocationContext context = AllocationFactory.eINSTANCE.createAllocationContext();

            context.setResourceContainer_AllocationContext(nodeType);
            context.setAssemblyContext_AllocationContext(computeJobAssembly);

            context.setAllocation_AllocationContext(allocation);
        }

        try {
            repositoryResource.save(null);
            systemResource.save(null);
            resourceEnvironmentResource.save(null);
            usageModelResource.save(null);
            allocationModelResource.save(null);
        } catch (IOException e) {
            System.out.println("Error while saving resources, e: " + e);
        }

        // for(NodeTypeDescription node : nodes) {
        // System.out.println(node);
        // }

        // File repoModelSource = FileHelper.getFile(repositoryPath.toFileString());
        //
        // final ResourceSet resourceSet = new ResourceSetImpl();
        // Resource resource = resourceSet.getResource(repositoryPath, true);
    }

    public void completeModels(final URI modelsPath) {
        System.out.println("Attempting to complete models ...");
    }

    public void completeRepositoryModel(Repository repository, List<JobTypeDescription> jobTypes) {

        List<RepositoryComponent> components = repository.getComponents__Repository();
        for (RepositoryComponent component : components) {
            System.out.println("Component found: " + component.getId());
            System.out.println("Component with name: " + component.getEntityName());
        }

        // TODO include more checks for correct model structure
        CompositeComponent computeJob = (CompositeComponent) findObjectWithId(components,
                COMPUTE_JOB_COMPOSITE_COMPONENT_ID);

        BasicComponent blueprintJob = (BasicComponent) findObjectWithId(components, BLUEPRINT_JOB_COMPONENT_ID);
        // System.out.println("Found basic Component: " +
        // EcoreUtil.getID(blueprintJob));

        ServiceEffectSpecification seff = findObjectWithId(
                blueprintJob.getServiceEffectSpecifications__BasicComponent(), BLUEPRINT_JOB_SEFF);

        // TODO Throw more meaningful exception to catch when calling this and notify
        // user about invalid model
        if (seff == null) {
            throw new IllegalArgumentException("Invalid model blueprint!");
        }

        ResourceDemandingSEFF resourceSeff = (ResourceDemandingSEFF) seff;

        // buildJobSEFF(resourceSeff, "test", jobTypes.get(0));

        for (JobTypeDescription job : jobTypes) {
            System.out.println("Adding job type to repository: " + job.getTypeName());
            buildAndAddJobComponentWithProvidedInterface(repository, job, resourceSeff, computeJob);
        }

        // BasicComponent copyJob = EcoreUtil.copy(blueprintJob);
        // System.out.println("ID of copy: " + EcoreUtil.getID(copyJob));
    }

    public BasicComponent buildAndAddJobComponentWithProvidedInterface(Repository repository,
            JobTypeDescription jobType, ResourceDemandingSEFF blueprintSeff, CompositeComponent computeJob) {

        BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();

        String jobTypeName = jobType.getTypeName();

        component.setEntityName(jobTypeName);
        repository.getComponents__Repository().add(component);

        // Create the interface with a single signature
        OperationInterface typeInterface = RepositoryFactory.eINSTANCE.createOperationInterface();
        typeInterface.setEntityName("interface_" + jobTypeName);

        // Add the interface to instance variable for later use
        jobInterfaces.put(jobTypeName, typeInterface);

        repository.getInterfaces__Repository().add(typeInterface);

        // Create a signature for the interface
        OperationSignature jobInterfaceSignature = RepositoryFactory.eINSTANCE.createOperationSignature();
        jobInterfaceSignature.setEntityName("run_" + jobTypeName);

        // Store for later use in Usage Model
        jobSignatures.put(jobTypeName, jobInterfaceSignature);

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

        providedRolesComputeJobAssembly.put(jobTypeName, compositeRole);

        // Create connector between composite component and basic component
        ProvidedDelegationConnector connector = CompositionFactory.eINSTANCE.createProvidedDelegationConnector();
        connector.setAssemblyContext_ProvidedDelegationConnector(assembly);
        // opProvidedRole is the role associated with the basic compute job
        // (implementing the interface)
        connector.setInnerProvidedRole_ProvidedDelegationConnector(opProvidedRole);
        connector.setOuterProvidedRole_ProvidedDelegationConnector(compositeRole);
        connector.setParentStructure__Connector(computeJob);

        return component;
    }

    public ResourceDemandingSEFF buildJobSEFF(ResourceDemandingSEFF blueprintSEFF, String seffName,
            JobTypeDescription jobType) {

        ResourceDemandingSEFF newSeff = EcoreUtil.copy(blueprintSEFF);
        EcoreUtil.setID(newSeff, EcoreUtil.generateUUID());

        InternalAction cpuDemandAction = null;
        PCMRandomVariable cpuDemandVariable = null;

        // Iterate through all the contents of the Seff to change IDs and find elements
        // to change
        TreeIterator<EObject> j = newSeff.eAllContents();
        while (j.hasNext()) {
            EObject obj = j.next();

            // Reset all IDs for contained objects that have IDs
            // Todo What is the clean way to do this?
            try {
                EcoreUtil.setID(obj, EcoreUtil.generateUUID());
            } catch (IllegalArgumentException e) {
                // Object does not have ID, do not reset
            }

            if (obj instanceof InternalAction) {
                cpuDemandAction = (InternalAction) obj;

            } else if (obj instanceof PCMRandomVariable) {
                cpuDemandVariable = (PCMRandomVariable) obj;
            }
        }

        cpuDemandVariable.setSpecification(jobType.getCpuDemandStoEx());

        return newSeff;
    }

    /**
     * Find the object with known ID in the list, return null if there is no such
     * object.
     * 
     * @param objects
     * @param id
     */
    private <T extends EObject> T findObjectWithId(List<T> objects, String id) {
        try {
            return objects.stream().filter(obj -> EcoreUtil.getID(obj).contentEquals(id)).findAny().get();
        } catch (NoSuchElementException e) {
            // Could not find matching element
            return null;
        }
    }

    /**
     * Find the object with know ID in all objects contained in the passed object,
     * null if there is no such object.
     * 
     * @param object
     * @param id
     * @return
     */
    private EObject findObjectWithIdRecursively(EObject object, String id) {

        if (id == null) {
            return null;
        }

        TreeIterator<EObject> i = object.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();

            String objId = EcoreUtil.getID(obj);

            if (id.equals(objId)) {
                return obj;
            }
        }
        return null;
    }

    private <T extends EObject> T copyChangeIds(T object) {

        // TODO This should return a deep copy including all containment references, is
        // this accurate?
        // TODO Maybe try this in a smaller setting to check?
        T result = EcoreUtil.copy(object);
        EcoreUtil.setID(result, EcoreUtil.generateUUID());

        TreeIterator<EObject> i = object.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();

            // Reset all IDs for contained objects that have IDs
            // Todo What is the clean way to do this?
            try {
                EcoreUtil.setID(obj, EcoreUtil.generateUUID());
            } catch (IllegalArgumentException e) {
                // Object does not have ID, do not reset
            }
        }
        return result;
    }

    private void changeIds(EObject object) {
        // Change the ID of the object itself
        EcoreUtil.setID(object, EcoreUtil.generateUUID());

        TreeIterator<EObject> i = object.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();

            // Reset all IDs for contained objects that have IDs
            // Todo What is the clean way to do this?
            try {
                EcoreUtil.setID(obj, EcoreUtil.generateUUID());
            } catch (IllegalArgumentException e) {
                // Object does not have ID, do not reset
            }
        }
    }
}
