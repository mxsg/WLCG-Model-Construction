package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.commons.eclipseutils.FileHelper;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.allocation.AllocationFactory;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.CompositionFactory;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.CompositeComponent;
import org.palladiosimulator.pcm.repository.Interface;
import org.palladiosimulator.pcm.repository.OperationInterface;
import org.palladiosimulator.pcm.repository.OperationProvidedRole;
import org.palladiosimulator.pcm.repository.OperationRequiredRole;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.resourceenvironment.ProcessingResourceSpecification;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.BranchAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.ForkAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.BranchTransition;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;

/**
 * Instances of this class can be used to calibrate Palladio simulation models by constructing them
 * from a blueprint Palladio model using model construction parameter objects.
 *
 * TODO Future work: Decouple model construction and blueprint model from model calibration plugin
 * and UI elements.
 *
 * TODO Future work: Split completion of the different models (may overcomplicate construction due
 * to interdependence between the models).
 *
 * @author Maximilian Stemmer-Grabow
 */
public class WLCGModelConstructor {

    // Required model files for completion
    private static final String REPO_MODEL_FILENAME = "jobs.repository";
    private static final String SYSTEM_MODEL_FILENAME = "jobs.system";
    private static final String RESOURCE_ENVIRONMENT_MODEL_FILENAME = "nodes.resourceenvironment";
    private static final String ALLOCATION_MODEL_FILENAME = "newAllocation.allocation";
    private static final String USAGE_MODEL_FILENAME = "wlcg.usagemodel";
    private static final String MONITOR_DIRECTORY_NAME = "monitor";
    private static final String MEASURINGPOINT_REPOSITORY_FILENAME = "measuringpoints.measuringpoint";
    private static final String MONITOR_REPOSITORY_FILENAME = "wlcg.monitorrepository";

    // IDs for the model elements used during construction
    private static final String COMPUTE_JOB_COMPOSITE_COMPONENT_ID = "WLCGBlueprint_computeJobCompositeComponent";
    private static final String BLUEPRINT_JOB_COMPONENT_ID = "WLCGBlueprint_blueprintJobComponent";

    private static final String BLUEPRINT_GRID_JOB_COMPONENT_ID = "gridJobComponent";
    private static final String BLUEPRINT_GRID_JOB_SEFF = "gridJobSEFF";

    private static final String GRID_JOB_INTERFACE = "gridJobInterface";
    private static final String BLUEPRINT_GRID_JOB_PROVIDED_ROLE = "gridJobProvidedRole";
    private static final String BLUEPRINT_GRID_JOB_ASSEMBLY = "gridJobAssembly";

    private static final String BLUEPRINT_JOB_SEFF = "WLCGBlueprint_runBlueprintJobSEFF";
    private static final String BLUEPRINT_JOB_EXTERNAL_CALL_ACTION = "jobExternalCallAction";

    private static final String BLUEPRINT_JOB_FORK = "WLCGBlueprint_runBlueprintJobSEFF_forkAction";
    private static final String BLUEPRINT_FORK_ACTION = "blueprintForkAction";

    private static final String COMPUTE_ASSEMBLY_CONTEXT_SYSTEM = "WLCGBlueprint_computeJobAssemblyContextSystem";
    private static final String BLUEPRINT_NODE = "WLCGBlueprint_blueprintNode";
    private static final String BLUEPRINT_CPU = "WLCGBlueprint_blueprintCPU";
    private static final String BLUEPRINT_HDD = "WLCGBlueprint_blueprintHDD";
    private static final String BLUEPRINT_CPU_MONITOR = "resourceMonitorCPU";
    private static final String BLUEPRINT_SYSTEM_CALL_MONITOR = "responseTimeTypeMonitor";
    private static final String BLUEPRINT_EXTERNAL_CALL_MONITOR = "externalCallResponseTimeMonitor";

    private static final String BLUEPRINT_USAGE_SCENARIO = "WLCGBlueprint_blueprintUsageScenario";
    private static final String BLUEPRINT_ENTRY_LEVEL_SYSTEM_CALL = "WLCGBlueprint_blueprintEntryLevelSystemCall";
    private static final String BLUEPRINT_USAGEMODEL_BRANCH_JOBTYPE = "branchUsageModelJobtype";

    // Used for holding model elements that are needed to connect models
    private Map<String, OperationInterface> jobInterfaces = new HashMap<>();
    private Map<String, OperationProvidedRole> providedRolesComputeJobAssembly = new HashMap<>();
    private Map<String, OperationProvidedRole> providedRolesSystem = new HashMap<>();
    private Map<String, OperationSignature> jobSignatures = new HashMap<>();
    private Map<String, ExternalCallAction> externalCallActions = new HashMap<>();

    private List<ResourceContainer> resourceContainerTypes = new ArrayList<>();
    private AssemblyContext computeJobAssembly = null;
    
    private static final boolean DUPLICATE_IO = true;
    private static final boolean FAST_IO  = false;
    private static final int IO_OVERALLOCATION = 1;
    private static final boolean JOBSLOTS_IO = true;
    
    private static final boolean USE_IO_RATIO = true;
    private static final int RESOURCE_ROUNDS = 10;

    /**
     * Create a simulation model construction object.
     */
    public WLCGModelConstructor() {
    }

    /**
     * Load model calibration parameters from a directory and use them to complete the simulation
     * model from a model blueprint. Models are modified and completed inplace.
     *
     * @param modelsPath
     *            The path to the directory the blueprint models are stored in.
     * @param parameterPath
     *            The path to the directory the model parameter files are stored in.
     */
    public void loadParametersAndCompleteModels(final URI modelsPath, final URI parameterPath) {
        // Find parameter files and import data
        String nodeDescriptionPath = parameterPath.appendSegment(Config.NODE_DESCRIPTION_FILENAME).toString();
        File nodeDescriptionFile = FileHelper.getFile(nodeDescriptionPath);

        List<NodeTypeDescription> nodes = new ArrayList<>();
        try {
            nodes = ParameterJSONImportHelper.readNodeTypes(nodeDescriptionFile);
        } catch (Exception e) {
            System.out.println("Something went wrong when importing node types! Error: " + e);
        }

        String jobTypeDescriptionPath = parameterPath.appendSegment(Config.JOBS_DESCRIPTION_FILENAME).toString();
        File jobDescriptionFile = FileHelper.getFile(jobTypeDescriptionPath);
        List<JobTypeDescription> jobs = new ArrayList<>();
        try {
            jobs = ParameterJSONImportHelper.readJobTypes(jobDescriptionFile);
        } catch (Exception e) {
            System.out.println("Something went wrong when importing jobs types! Error: " + e);
        }

        // Attempt to complete models
        completeModels(modelsPath, nodes, jobs);

    }

    /**
     * Complete simulation models from node and job type descriptions. This inserts job types into
     * the models and creates a resource environment from the node type descriptions provided.
     * Models are completed inplace.
     *
     * @param modelsPath
     *            The path to the models to be completed. These are changed inplace.
     * @param nodes
     *            The list of node types to be inserted into the model.
     * @param jobs
     *            The list of job types to be inserted into the model.
     */
    public void completeModels(final URI modelsPath, List<NodeTypeDescription> nodes, List<JobTypeDescription> jobs) {

        ResourceSet resourceSet = new ResourceSetImpl();

        URI repositoryPath = modelsPath.appendSegment(REPO_MODEL_FILENAME);
        Resource repositoryResource = resourceSet.getResource(repositoryPath, true);
        Repository repository = (Repository) repositoryResource.getContents().get(0);

        // Complete the repository model
        this.completeRepositoryModel(repository, jobs);

        // Load monitor repositories
        URI measuringpointPath = modelsPath.appendSegment(MONITOR_DIRECTORY_NAME)
                .appendSegment(MEASURINGPOINT_REPOSITORY_FILENAME);
        Resource measuringpointResource = resourceSet.getResource(measuringpointPath, true);
        MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) measuringpointResource.getContents()
                .get(0);

        URI monitorPath = modelsPath.appendSegment(MONITOR_DIRECTORY_NAME).appendSegment(MONITOR_REPOSITORY_FILENAME);
        Resource monitorResource = resourceSet.getResource(monitorPath, true);
        MonitorRepository monitorRepo = (MonitorRepository) monitorResource.getContents().get(0);

        // Complete the system model
        Resource systemResource = resourceSet.getResource(modelsPath.appendSegment(SYSTEM_MODEL_FILENAME), true);
        org.palladiosimulator.pcm.system.System system = (org.palladiosimulator.pcm.system.System) systemResource
                .getContents().get(0);

        this.completeSystemModel(system, jobs, monitorRepo, measuringPointRepo);

        // Load and complete resource environment
        Resource resourceEnvironmentResource = resourceSet
                .getResource(modelsPath.appendSegment(RESOURCE_ENVIRONMENT_MODEL_FILENAME), true);
        ResourceEnvironment resEnv = (ResourceEnvironment) resourceEnvironmentResource.getContents().get(0);

        this.completeResourceEnvironment(resEnv, nodes, monitorRepo, measuringPointRepo);

        // Complete the Usage Model
        Resource usageModelResource = resourceSet.getResource(modelsPath.appendSegment(USAGE_MODEL_FILENAME), true);
        UsageModel usageModel = (UsageModel) usageModelResource.getContents().get(0);

        // Retrieve usage scenario response time monitor
        List<Monitor> allMonitors = monitorRepo.getMonitors();
        Monitor blueprintResponseTimeMonitor = ModelConstructionUtils.findObjectWithId(allMonitors,
                BLUEPRINT_SYSTEM_CALL_MONITOR);

        if (blueprintResponseTimeMonitor == null) {
            throw new IllegalArgumentException("Invalid monitor repository, missing entry level system call monitor.");
        }

        completeUsageModel(usageModel, jobs, blueprintResponseTimeMonitor, monitorRepo, measuringPointRepo);

        // Remove blueprint monitor from monitor repository
        blueprintResponseTimeMonitor.setMonitorRepository(null);


        // Insert monitors for job response times
        Monitor blueprintJobResponseTimeMonitor = ModelConstructionUtils
                .findObjectWithId(allMonitors, BLUEPRINT_EXTERNAL_CALL_MONITOR);

        if (blueprintJobResponseTimeMonitor == null) {
            throw new IllegalArgumentException("Invalid monitor repository, missing external call monitor.");
        }

        for (Map.Entry<String, ExternalCallAction> entry : this.externalCallActions.entrySet()) {
            this.addExternalCallMonitoring(measuringPointRepo, monitorRepo, entry.getValue(),
                    blueprintJobResponseTimeMonitor, entry.getKey());
        }

        // Remove blueprint monitor from monitor repository
        blueprintJobResponseTimeMonitor.setMonitorRepository(null);


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

        // Save all modified models
        try {
            repositoryResource.save(null);
            systemResource.save(null);
            resourceEnvironmentResource.save(null);
            usageModelResource.save(null);
            allocationModelResource.save(null);
            measuringpointResource.save(null);
            monitorResource.save(null);
        } catch (IOException e) {
            System.out.println("Error while saving resources, e: " + e);
        }
    }

    /**
     * Complete the repository model by inserting components for each job type that is supplied in
     * the list of job type descriptions.
     *
     * @param repository
     *            The repository model the jobs should be inserted into.
     * @param jobTypes
     *            The job types to be inserted.
     */
    private void completeRepositoryModel(Repository repository, List<JobTypeDescription> jobTypes) {

        List<RepositoryComponent> components = repository.getComponents__Repository();

        CompositeComponent computeJob = (CompositeComponent) ModelConstructionUtils.findObjectWithId(components,
                COMPUTE_JOB_COMPOSITE_COMPONENT_ID);

        BasicComponent blueprintJob = (BasicComponent) ModelConstructionUtils.findObjectWithId(components,
                BLUEPRINT_JOB_COMPONENT_ID);

        List<Stereotype> blueprintStereotypes = StereotypeAPI.getAppliedStereotypes(blueprintJob);

        Stereotype middlewareDependencyStereotype = blueprintStereotypes.stream()
                .filter(stereotype -> "MiddlewareDependency".equals(stereotype.getName())).findAny().orElse(null);

        if (middlewareDependencyStereotype == null) {
            throw new IllegalArgumentException("Invalid model blueprint, missing middleware dependency stereotype!");
        }

        ServiceEffectSpecification seff = ModelConstructionUtils
                .findObjectWithId(blueprintJob.getServiceEffectSpecifications__BasicComponent(), BLUEPRINT_JOB_SEFF);

        if (seff == null) {
            throw new IllegalArgumentException("Invalid model blueprint!");
        }

        // Get the grid job Interface
        List<Interface> interfaces = repository.getInterfaces__Repository();
        OperationInterface gridJobInterface = (OperationInterface) ModelConstructionUtils.findObjectWithId(interfaces, GRID_JOB_INTERFACE);

        if (gridJobInterface == null) {
            throw new IllegalArgumentException("Invalid model blueprint, missing grid job interface!");
        }

        // Complete SEFF for Grid Job Component
        BasicComponent gridJob = (BasicComponent) ModelConstructionUtils
                .findObjectWithId(components, BLUEPRINT_GRID_JOB_COMPONENT_ID);

        OperationProvidedRole gridJobProvidedRole = (OperationProvidedRole) ModelConstructionUtils
                .findObjectWithId(gridJob.getProvidedRoles_InterfaceProvidingEntity(), BLUEPRINT_GRID_JOB_PROVIDED_ROLE);

        ServiceEffectSpecification gridJobSeff = ModelConstructionUtils
                .findObjectWithId(gridJob.getServiceEffectSpecifications__BasicComponent(), BLUEPRINT_GRID_JOB_SEFF);
        ResourceDemandingSEFF resourceGridJobSeff = (ResourceDemandingSEFF) gridJobSeff;

        constructMultithreadedSEFF(resourceGridJobSeff);

        // Build the job type components and add them to the models
        ResourceDemandingSEFF resourceSeff = (ResourceDemandingSEFF) seff;
        for (JobTypeDescription job : jobTypes) {
            System.out.println("Adding job type to repository: " + job.getTypeName());
            buildAndAddJobComponentWithProvidedInterface(repository, job, resourceSeff, computeJob,
                    middlewareDependencyStereotype, gridJobInterface, gridJobProvidedRole);
        }
    }

    private void constructMultithreadedSEFF(ResourceDemandingSEFF seff) {

        ForkAction forkAction = null;
        EObject forkObject = ModelConstructionUtils.findObjectWithIdRecursively(seff, BLUEPRINT_FORK_ACTION);
        if (forkObject instanceof ForkAction) {
            forkAction = (ForkAction) forkObject;
        }

        if (forkAction == null) {
            throwNewInvalidModelException("Repository", "Could not find fork action in job RDSEFF.");
        }

        AbstractAction predecessorAction = forkAction.getPredecessor_AbstractAction();
        AbstractAction successorAction = forkAction.getSuccessor_AbstractAction();

        // Remove fork from SEFF
        forkAction.setResourceDemandingBehaviour_AbstractAction(null);

        BranchAction branch = ModelConstructionUtils.duplicateBehaviours(forkAction, "threadCount", 8);
        branch.setResourceDemandingBehaviour_AbstractAction(seff);

        branch.setPredecessor_AbstractAction(predecessorAction);
        branch.setSuccessor_AbstractAction(successorAction);
    }

    /**
     * Complete the system model by connecting roles and interfaces associated to the inserted job
     * types from the repository model.
     *
     * @param system
     *            The system model.
     * @param jobs
     *            The descriptions of included job types.
     * @param monitorRepo
     *            The monitor repository model.
     * @param measuringPointRepo
     *            The measuring point repository model.
     */
    private void completeSystemModel(org.palladiosimulator.pcm.system.System system, List<JobTypeDescription> jobs,
            MonitorRepository monitorRepo, MeasuringPointRepository measuringPointRepo) {

        List<AssemblyContext> systemAssemblies = system.getAssemblyContexts__ComposedStructure();
        AssemblyContext computeAssembly = ModelConstructionUtils.findObjectWithId(systemAssemblies,
                COMPUTE_ASSEMBLY_CONTEXT_SYSTEM);

        // Used later to connect interfaces with usage model
        this.computeJobAssembly = computeAssembly;

        for (JobTypeDescription jobDescription : jobs) {
            String jobTypeName = jobDescription.getTypeName();

            OperationInterface jobInterface = jobInterfaces.get(jobTypeName);
            OperationProvidedRole assemblyProvidedRole = providedRolesComputeJobAssembly.get(jobTypeName);

            // Create new system provided role and add to system model
            OperationProvidedRole role = RepositoryFactory.eINSTANCE.createOperationProvidedRole();
            role.setEntityName("provided_role_system_" + jobTypeName);
            role.setProvidedInterface__OperationProvidedRole(jobInterface);

            this.providedRolesSystem.put(jobTypeName, role);
            role.setProvidingEntity_ProvidedRole(system);

            // Create a measuring point for each system operation

            SystemOperationMeasuringPoint systemOperationMp = PcmmeasuringpointFactory.eINSTANCE
                    .createSystemOperationMeasuringPoint();
            systemOperationMp.setOperationSignature(this.jobSignatures.get(jobTypeName));
            systemOperationMp.setRole(role);
            systemOperationMp.setSystem(system);

            systemOperationMp.setMeasuringPointRepository(measuringPointRepo);

            // Create corresponding monitor

            // Find the original monitor from the monitoring repository
            List<Monitor> monitors = monitorRepo.getMonitors();
            Monitor blueprintSystemMonitor = ModelConstructionUtils.findObjectWithId(monitors,
                    "systemOperationMonitor");

            if (blueprintSystemMonitor == null) {
                throw new IllegalArgumentException("Invalid monitor repository, missing system operation monitor.");
            }

            Monitor duplicatedSystemMonitor = ModelConstructionUtils.copyAppendIds(blueprintSystemMonitor, jobTypeName);

            String systemMonitorName = MessageFormat.format("Response Time Monitor System Operation {0}", jobTypeName);
            duplicatedSystemMonitor.setEntityName(systemMonitorName);

            addMonitorToModel(measuringPointRepo, monitorRepo, systemOperationMp, duplicatedSystemMonitor);

            ProvidedDelegationConnector connector = CompositionFactory.eINSTANCE.createProvidedDelegationConnector();
            connector.setOuterProvidedRole_ProvidedDelegationConnector(role);
            connector.setInnerProvidedRole_ProvidedDelegationConnector(assemblyProvidedRole);
            connector.setAssemblyContext_ProvidedDelegationConnector(computeAssembly);

            // Add connector to system model
            connector.setParentStructure__Connector(system);
        }
    }

    /**
     * Complete the resource environment by inserting the node types included in the node type list.
     * Also duplicate measuring points and monitors for each added node type.
     *
     * @param resEnv
     *            The resource environment to complete.
     * @param nodes
     *            A list of node types to be included in the resource environment.
     * @param monitorRepo
     *            The monitor repository the duplicated monitors should be added to.
     * @param measuringPointRepo
     *            The measuring point repository the duplicated measuring points should be added to.
     */
    private void completeResourceEnvironment(ResourceEnvironment resEnv, List<NodeTypeDescription> nodes,
            MonitorRepository monitorRepo, MeasuringPointRepository measuringPointRepo) {

        List<ResourceContainer> resourceContainers = resEnv.getResourceContainer_ResourceEnvironment();
        ResourceContainer blueprintContainer = ModelConstructionUtils.findObjectWithId(resourceContainers,
                BLUEPRINT_NODE);

        if (blueprintContainer == null) {
            throw new IllegalArgumentException(
                    "Invalid resource environment model, missing blueprint resource container.");
        }

        // Find blueprint stereotypes
        List<Stereotype> blueprintStereotypes = StereotypeAPI.getAppliedStereotypes(blueprintContainer);

        Stereotype loadBalancedResourceContainerStereotype = blueprintStereotypes.stream()
                .filter(stereotype -> "StaticLoadbalancedResourceContainer".equals(stereotype.getName())).findAny()
                .orElse(null);

        Stereotype middlewareHostStereotype = blueprintStereotypes.stream()
                .filter(stereotype -> "MiddlewareHost".contentEquals(stereotype.getName())).findAny().orElse(null);

        if (loadBalancedResourceContainerStereotype == null || middlewareHostStereotype == null) {
            throw new IllegalArgumentException("Invalid model blueprint, missing stereotypes on resource container!");
        }

        // Find the original monitor from the monitoring repository
        List<Monitor> allMonitors = monitorRepo.getMonitors();
        Monitor blueprintCpuMonitor = ModelConstructionUtils.findObjectWithId(allMonitors, BLUEPRINT_CPU_MONITOR);

        if (blueprintCpuMonitor == null) {
            throw new IllegalArgumentException("Invalid monitor repository, missing CPU resource monitor.");
        }

        Monitor blueprintResponseTimeMonitor = ModelConstructionUtils.findObjectWithId(allMonitors,
                BLUEPRINT_SYSTEM_CALL_MONITOR);

        if (blueprintResponseTimeMonitor == null) {
            throw new IllegalArgumentException("Invalid monitor repository, missing entry level system call monitor.");
        }

        for (NodeTypeDescription nodeType : nodes) {
            String nodeTypeName = nodeType.getName();

            ResourceContainer newNode = EcoreUtil.copy(blueprintContainer);

            newNode.setEntityName(nodeTypeName);

            List<ProcessingResourceSpecification> resourceSpecs = newNode
                    .getActiveResourceSpecifications_ResourceContainer();

            ProcessingResourceSpecification cpuResourceSpec = ModelConstructionUtils.findObjectWithId(resourceSpecs,
                    BLUEPRINT_CPU);

            ProcessingResourceSpecification hddResourceSpec = ModelConstructionUtils.findObjectWithId(resourceSpecs,
                    BLUEPRINT_HDD);

            // Set CPU properties
            cpuResourceSpec.setNumberOfReplicas(nodeType.getCores());

            PCMRandomVariable processingRate = CoreFactory.eINSTANCE.createPCMRandomVariable();
            processingRate.setSpecification(String.valueOf(nodeType.getComputingRate()));

            cpuResourceSpec.setProcessingRate_ProcessingResourceSpecification(processingRate);

            // Set I/O properties
            int ioReplicas = 1;
            if (DUPLICATE_IO) {
            	ioReplicas = nodeType.getCores();
                if (JOBSLOTS_IO) {
                	ioReplicas = nodeType.getJobslots();
                }
            }
            hddResourceSpec.setNumberOfReplicas(ioReplicas);
            
            
            PCMRandomVariable processingRateHDD = CoreFactory.eINSTANCE.createPCMRandomVariable();
            
            int ioRate = 1;
            if (FAST_IO) {
            	ioRate = nodeType.getCores();
            	if (JOBSLOTS_IO) {
            		ioRate = nodeType.getJobslots();
            	}
            }
            ioRate *= IO_OVERALLOCATION;
            processingRateHDD.setSpecification(String.valueOf(ioRate));

            
            hddResourceSpec.setProcessingRate_ProcessingResourceSpecification(processingRateHDD);
            hddResourceSpec.setResourceContainer_ProcessingResourceSpecification(newNode);

            // Add new measuring points for the new resource
            addMeasuringpointsAndMonitors(measuringPointRepo, monitorRepo, cpuResourceSpec, blueprintCpuMonitor,
                    nodeTypeName);

            // Change all IDs for the copied objects to avoid conflicts
            ModelConstructionUtils.setRandomIDsRecursively(newNode);

            // Add new resource container to tracked resource containers
            this.resourceContainerTypes.add(newNode);

            // Add new node to resource environment
            newNode.setResourceEnvironment_ResourceContainer(resEnv);

            // Add stereotypes for Architectural Template Application
            // This needs to be done last so that the new node is already included in a
            // resource
            StereotypeAPI.applyStereotype(newNode, loadBalancedResourceContainerStereotype);

            // Set duplication number to correct value
            StereotypeAPI.setTaggedValue(newNode, nodeType.getNodeCount(), "StaticLoadbalancedResourceContainer",
                    "numberOfReplicas");

            StereotypeAPI.applyStereotype(newNode, middlewareHostStereotype);
            StereotypeAPI.setTaggedValue(newNode, nodeType.getJobslots(), "MiddlewareHost", "capacity");
        }
    }

    /**
     * Complete the usage scenario by adding a branch for each job type in the provided list into
     * the usage model. Also add measuring points and monitors for response time of each of the
     * added job types.
     *
     * @param usageModel
     *            The usage model to be completed.
     * @param jobs
     *            A list of job types to be inserted into the
     * @param responseTimeMonitor
     *            The blueprint monitor to be duplicated for response time measurements.
     * @param monitorRepo
     *            The monitor repository the created monitors are to be inserted into.
     * @param measuringpointRepo
     *            The measuring point repository the created measuring points are to be inserted
     *            into.
     */
    private void completeUsageModel(UsageModel usageModel, List<JobTypeDescription> jobs, Monitor responseTimeMonitor,
            MonitorRepository monitorRepo, MeasuringPointRepository measuringpointRepo) {

        List<UsageScenario> usageScenarios = usageModel.getUsageScenario_UsageModel();
        UsageScenario blueprintUsageScenario = ModelConstructionUtils.findObjectWithId(usageScenarios,
                BLUEPRINT_USAGE_SCENARIO);

        EObject jobtypeBranchObject = ModelConstructionUtils.findObjectWithIdRecursively(blueprintUsageScenario,
                BLUEPRINT_USAGEMODEL_BRANCH_JOBTYPE);
        Branch jobtypeBranch = null;
        if (jobtypeBranchObject instanceof Branch) {
            jobtypeBranch = (Branch) jobtypeBranchObject;
        }

        if (jobtypeBranch == null) {
            throw new IllegalArgumentException("Invalid Usage Model: Could not find job type branch!");
        }

        // The blueprint model only has one branch transition
        BranchTransition blueprintTransition = null;
        List<BranchTransition> blueprintTransitionList = jobtypeBranch.getBranchTransitions_Branch();

        if (blueprintTransitionList.size() < 1) {
            throwNewInvalidModelException("Usage Model", "Could not find branch transition.");
        }
        blueprintTransition = blueprintTransitionList.get(0);

        for (JobTypeDescription jobType : jobs) {
            String jobTypeName = jobType.getTypeName();

            BranchTransition newTransition = EcoreUtil.copy(blueprintTransition);

            // TODO Maybe normalize or check the values here again in case of invalid JSON data?

            // Set correct branch probability
            newTransition.setBranchProbability(jobType.getRelativeFrequency());

            ScenarioBehaviour blueprintBehaviour = blueprintTransition.getBranchedBehaviour_BranchTransition();
            if (blueprintBehaviour == null) {
                throwNewInvalidModelException("usage", "Could not find branched scenario behaviour!");
            }

            EObject systemCallObject = ModelConstructionUtils.findObjectWithIdRecursively(newTransition,
                    BLUEPRINT_ENTRY_LEVEL_SYSTEM_CALL);
            EntryLevelSystemCall systemCall = null;
            if (systemCallObject instanceof EntryLevelSystemCall) {
                systemCall = (EntryLevelSystemCall) systemCallObject;
            }

            systemCall.setOperationSignature__EntryLevelSystemCall(jobSignatures.get(jobTypeName));
            systemCall.setProvidedRole_EntryLevelSystemCall(providedRolesSystem.get(jobTypeName));

            // For now, do not add system call monitor and measuring points.
            // Measurement functionality is instead provided by the measuring points located in the
            // system model.

            // addSystemCallMonitor(measuringpointRepo, monitorRepo, systemCall,
            // responseTimeMonitor, jobTypeName);

            // Change all IDs for the new branch transition
            ModelConstructionUtils.appendIDsRecursively(newTransition, jobTypeName);

            newTransition.setBranch_BranchTransition(jobtypeBranch);
        }

        // Remove blueprint branch transition from the usage model. This is important to not alter
        // the generated load profile.
        blueprintTransition.setBranch_BranchTransition(null);
    }

    /**
     * Duplicate the measuring points and monitors for each core of the provided processing spec.
     *
     * @param measuringPointRepo
     *            The repository the duplicated measuring point should be inserted into.
     * @param monitorRepo
     *            The repository the duplicated monitors should be inserted into.
     * @param processingSpec
     *            The processing spec to be equipped with measuring points and monitors.
     * @param originalMonitor
     *            The blueprint monitor used to duplicate monitors.
     * @param additionalSuffix
     *            An additional suffix to be included in the IDs of the duplicated monitors.
     */
    private void addMeasuringpointsAndMonitors(MeasuringPointRepository measuringPointRepo,
            MonitorRepository monitorRepo, ProcessingResourceSpecification processingSpec, Monitor originalMonitor,
            String additionalSuffix) {

        if (additionalSuffix == null) {
            additionalSuffix = "";
        }

        int coreCount = processingSpec.getNumberOfReplicas();

        // Add a new CPU measuring point for each core
        for (int i = 0; i < coreCount; i++) {
            ActiveResourceMeasuringPoint point = PcmmeasuringpointFactory.eINSTANCE
                    .createActiveResourceMeasuringPoint();
            point.setActiveResource(processingSpec);
            point.setReplicaID(i);
            point.setMeasuringPointRepository(measuringPointRepo);

            // Add a monitor for each new measuring point
            Monitor duplicatedMonitor = ModelConstructionUtils.copyAppendIds(originalMonitor,
                    "_" + additionalSuffix + "_core" + i);

            duplicatedMonitor.setMeasuringPoint(point);
            duplicatedMonitor.setMonitorRepository(monitorRepo);

            String monitorName = MessageFormat.format("CPU Monitor {0} (core {1})", additionalSuffix, i);
            duplicatedMonitor.setEntityName(monitorName);

            duplicatedMonitor.setActivated(true);
        }

    }

    private void addExternalCallMonitoring(MeasuringPointRepository measuringPointRepo, MonitorRepository monitorRepo, ExternalCallAction externalCall,
            Monitor originalMonitor, String additionalSuffix) {

        if (additionalSuffix == null) {
            additionalSuffix = "";
        }

        // Add a new measuring point for the external call
        ExternalCallActionMeasuringPoint point = PcmmeasuringpointFactory.eINSTANCE.createExternalCallActionMeasuringPoint();
        point.setExternalCall(externalCall);
        point.setMeasuringPointRepository(measuringPointRepo);

        // Add a monitor for the new measuring point
        Monitor duplicatedMonitor = ModelConstructionUtils.copyAppendIds(originalMonitor, "_" + additionalSuffix);

        duplicatedMonitor.setMeasuringPoint(point);
        duplicatedMonitor.setMonitorRepository(monitorRepo);

        duplicatedMonitor.setEntityName("Job Response Time Monitor " + additionalSuffix);

        duplicatedMonitor.setActivated(true);
    }

    /**
     * Add the monitor and measuring point to their respective repositories, connect and activate
     * monitor and measuring point.
     *
     * @param measuringPointRepo
     *            The repository the measuring point should be added to.
     * @param monitorRepo
     *            The repository the monitor should be added to.
     * @param measuringPoint
     *            The measuring point to add and connect to its monitor.
     * @param monitor
     *            The monitor to add and connect to the measuring point.
     */
    private void addMonitorToModel(MeasuringPointRepository measuringPointRepo, MonitorRepository monitorRepo,
            MeasuringPoint measuringPoint, Monitor monitor) {

        measuringPoint.setMeasuringPointRepository(measuringPointRepo);

        monitor.setMeasuringPoint(measuringPoint);
        monitor.setMonitorRepository(monitorRepo);
        monitor.setActivated(true);
    }

    /**
     * Build a new basic component from a job type description, include it in a composite compute
     * component and add it to a repository model.
     *
     * @param repository
     *            The repository model the component should be added to.
     * @param jobType
     *            The job type description the new component should be based on.
     * @param blueprintSeff
     *            The SEFF to be included in the component.
     * @param computeJob
     *            The composite compute component the basic component role should be added to.
     * @param stereotypeToApply
     *            A stereotype to be added to the component. No stereotype will be added if this is
     *            null.
     * @return A new basic component created from the job type description.
     */
    private BasicComponent buildAndAddJobComponentWithProvidedInterface(Repository repository,
            JobTypeDescription jobType, ResourceDemandingSEFF blueprintSeff, CompositeComponent computeJob,
            Stereotype stereotypeToApply, OperationInterface requiredJobInterface, OperationProvidedRole gridJobProvidedRole) {

        BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();

        String jobTypeName = jobType.getTypeName();

        component.setEntityName(jobTypeName);
        repository.getComponents__Repository().add(component);

        if (stereotypeToApply != null) {
            StereotypeAPI.applyStereotype(component, stereotypeToApply);

            StereotypeAPI.setTaggedValue(component, jobType.getRequiredJobslotsStoEx(), "MiddlewareDependency",
                    "numberRequiredResources");
            StereotypeAPI.setTaggedValue(component, jobType.getSchedulingDelay(), "MiddlewareDependency",
                    "schedulingDelay");
        }

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
        opProvidedRole.setEntityName("provided_role_component_" + jobTypeName);

        // Set the interface for the role
        opProvidedRole.setProvidedInterface__OperationProvidedRole(typeInterface);

        // Add the provided role to the component
        component.getProvidedRoles_InterfaceProvidingEntity().add(opProvidedRole);


        // Add required role to the component
        OperationRequiredRole requiredRole = RepositoryFactory.eINSTANCE.createOperationRequiredRole();
        requiredRole.setEntityName("required_role_component_" + jobTypeName);

        // Set the interface for the required role
        requiredRole.setRequiredInterface__OperationRequiredRole(requiredJobInterface);

        // Add the required role to the component as well
        component.getRequiredRoles_InterfaceRequiringEntity().add(requiredRole);


        ResourceDemandingSEFF seff = EcoreUtil.copy(blueprintSeff);
        ExternalCallAction externalCall = (ExternalCallAction) ModelConstructionUtils.findObjectWithId(seff.getSteps_Behaviour(), BLUEPRINT_JOB_EXTERNAL_CALL_ACTION);

        // Set the correct role for the job component's external call to the generic grid job
        externalCall.setRole_ExternalService(requiredRole);
        externalCall.setEntityName("externalCallType_" + jobTypeName);

        // Save for duplicated monitors
        this.externalCallActions.put(jobTypeName, externalCall);

        ModelConstructionUtils.appendIDsRecursively(seff, "_" + jobTypeName);

        seff.setDescribedService__SEFF(jobInterfaceSignature);

        // Add SEFF to component
        component.getServiceEffectSpecifications__BasicComponent().add(seff);

        // Add variables to component
        VariableUsage cpuVariableUsage = ModelConstructionUtils.createVariableUsageWithValue("CPU_DEMAND",
                jobType.getCpuDemandStoEx());
        component.getComponentParameterUsage_ImplementationComponentType().add(cpuVariableUsage);

        VariableUsage ioTimeRatioUsage = ModelConstructionUtils.createVariableUsageWithValue("IO_RATIO",
                jobType.getIoTimeRatioStoEx());
        component.getComponentParameterUsage_ImplementationComponentType().add(ioTimeRatioUsage);

        // TODO This should be depending on the job length
        VariableUsage ioTimeVariableUsage = ModelConstructionUtils.createVariableUsageWithValue("IO_DEMAND",
                jobType.getIoTimeStoEx());
        component.getComponentParameterUsage_ImplementationComponentType().add(ioTimeVariableUsage);

        VariableUsage resourceDemandRounds = ModelConstructionUtils
                .createVariableUsageWithValue("RESOURCE_DEMAND_ROUNDS", Integer.toString(RESOURCE_ROUNDS));
        component.getComponentParameterUsage_ImplementationComponentType().add(resourceDemandRounds);

        // Add component to repository
        repository.getComponents__Repository().add(component);

        // Add component to computing job component
        AssemblyContext assembly = CompositionFactory.eINSTANCE.createAssemblyContext();
        assembly.setEncapsulatedComponent__AssemblyContext(component);
        assembly.setParentStructure__AssemblyContext(computeJob);

        assembly.setEntityName("assembly_context_" + jobTypeName);

        // Add dependent parameter usage
        VariableUsage ioFromRatioVariableUsage = null;
        if (USE_IO_RATIO) {
            ioFromRatioVariableUsage = ModelConstructionUtils
                    .createVariableUsageWithValue("IO_DEMAND_FROM_RATIO", "IO_RATIO.VALUE * CPU_DEMAND.VALUE");
        } else {
            ioFromRatioVariableUsage = ModelConstructionUtils
                    .createVariableUsageWithValue("IO_DEMAND_FROM_RATIO", "IO_DEMAND.VALUE");

        }
        
        assembly.getConfigParameterUsages__AssemblyContext().add((ioFromRatioVariableUsage));

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

        // Create connection between basic component and grid job component
        List<AssemblyContext> computeJobAssemblies = computeJob.getAssemblyContexts__ComposedStructure();
        AssemblyContext gridJobAssembly = ModelConstructionUtils.findObjectWithId(computeJobAssemblies, BLUEPRINT_GRID_JOB_ASSEMBLY);

        AssemblyConnector gridJobConnector = CompositionFactory.eINSTANCE.createAssemblyConnector();
        gridJobConnector.setProvidedRole_AssemblyConnector(gridJobProvidedRole);
        gridJobConnector.setRequiredRole_AssemblyConnector(requiredRole);

        gridJobConnector.setProvidingAssemblyContext_AssemblyConnector(gridJobAssembly);
        gridJobConnector.setRequiringAssemblyContext_AssemblyConnector(assembly);

        gridJobConnector.setParentStructure__Connector(computeJob);

        return component;
    }

    /**
     * Throw an exception indicating a blueprint model does not match the expected structure.
     *
     * @param affectedModel
     *            A description of the affected model.
     * @param message
     *            A message that describes why the model is invalid.
     */
    private void throwNewInvalidModelException(String affectedModel, String message) {
        String failureMessage = MessageFormat.format("Invalid {0} model: {1}", affectedModel, message);
        throw new IllegalArgumentException(failureMessage);
    }
}
