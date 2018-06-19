package org.palladiosimulator.wlcgmodel;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.TreeIterator;
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
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.composition.CompositionFactory;
import org.palladiosimulator.pcm.core.composition.ProvidedDelegationConnector;
import org.palladiosimulator.pcm.parameter.VariableUsage;
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
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.BranchAction;
import org.palladiosimulator.pcm.seff.ForkAction;
import org.palladiosimulator.pcm.seff.ForkedBehaviour;
import org.palladiosimulator.pcm.seff.GuardedBranchTransition;
import org.palladiosimulator.pcm.seff.InternalAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.seff.SynchronisationPoint;
import org.palladiosimulator.pcm.seff.seff_performance.ParametricResourceDemand;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.BranchTransition;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcmmeasuringpoint.ActiveResourceMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.EntryLevelSystemCallMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;

public class PCMModelCompletion {

    private static final String REPO_MODEL_FILENAME = "jobs.repository";
    private static final String SYSTEM_MODEL_FILENAME = "jobs.system";
    private static final String RESOURCE_ENVIRONMENT_MODEL_FILENAME = "nodes.resourceenvironment";
    private static final String ALLOCATION_MODEL_FILENAME = "newAllocation.allocation";
    private static final String USAGE_MODEL_FILENAME = "wlcg.usagemodel";
    private static final String MONITOR_DIRECTORY_NAME = "monitor";
    private static final String MEASURINGPOINT_REPOSITORY_FILENAME = "measuringpoints.measuringpoint";
    private static final String MONITOR_REPOSITORY_FILENAME = "wlcg.monitorrepository";

    private static final String COMPUTE_JOB_COMPOSITE_COMPONENT_ID = "WLCGBlueprint_computeJobCompositeComponent";
    private static final String BLUEPRINT_JOB_COMPONENT_ID = "WLCGBlueprint_blueprintJobComponent";
    private static final String BLUEPRINT_JOB_SEFF = "WLCGBlueprint_runBlueprintJobSEFF";
    // private static final String BLUEPRINT_JOB_SEFF_INTERNAL_ACTION =
    // "WLCGBlueprint_runBlueprintJobSEFF_internalAction";
    private static final String BLUEPRINT_JOB_CPU_ACTION = "WLCGBlueprint_runBlueprintJobSEFF_cpuAction";
    private static final String BLUEPRINT_JOB_IO_ACTION = "WLCGBlueprint_runBlueprintJobSEFF_ioAction";
    private static final String BLUEPRINT_JOB_FORK = "WLCGBlueprint_runBlueprintJobSEFF_forkAction";

    // private static final String BLUEPRINT_JOB_SEFF_INTERNAL_CPU_RESOURCE_TYPE =
    // "WLCGBlueprint_runBlueprintJobSEFF_cpu_resourcetype";
    private static final String COMPUTE_ASSEMBLY_CONTEXT_SYSTEM = "WLCGBlueprint_computeJobAssemblyContextSystem";
    private static final String BLUEPRINT_NODE = "WLCGBlueprint_blueprintNode";
    private static final String BLUEPRINT_CPU = "WLCGBlueprint_blueprintCPU";
    // private static final String BLUEPRINT_HDD = "WLCGBlueprint_blueprintHDD";
    private static final String BLUEPRINT_CPU_MONITOR = "resourceMonitorCPU";
    private static final String BLUEPRINT_SYSTEM_CALL_MONITOR = "responseTimeTypeMonitor";

    private static final String BLUEPRINT_USAGE_SCENARIO = "WLCGBlueprint_blueprintUsageScenario";
    private static final String BLUEPRINT_ENTRY_LEVEL_SYSTEM_CALL = "WLCGBlueprint_blueprintEntryLevelSystemCall";
    private static final String BLUEPRINT_USAGEMODEL_BRANCH_JOBTYPE = "branchUsageModelJobtype";

    // Used for holding model elements that are later needed to connect models
    private Map<String, OperationInterface> jobInterfaces = new HashMap<>();
    private Map<String, OperationProvidedRole> providedRolesComputeJobAssembly = new HashMap<>();
    private Map<String, OperationProvidedRole> providedRolesSystem = new HashMap<>();
    private Map<String, OperationSignature> jobSignatures = new HashMap<>();

    private List<ResourceContainer> resourceContainerTypes = new ArrayList<>();
    private AssemblyContext computeJobAssembly = null;

    public PCMModelCompletion() {
    }

    public void completeModels(final URI modelsPath, List<NodeTypeDescription> nodes, List<JobTypeDescription> jobs) {

        // For now, only import the repository model
        URI repositoryPath = modelsPath.appendSegment(REPO_MODEL_FILENAME);

        ResourceSet resourceSet = new ResourceSetImpl();
        Resource repositoryResource = resourceSet.getResource(repositoryPath, true);
        Repository repository = (Repository) repositoryResource.getContents().get(0);

        // Complete repository model

        completeRepositoryModel(repository, jobs);

        // Get monitor repositories
        URI measuringpointPath = modelsPath.appendSegment(MONITOR_DIRECTORY_NAME)
                .appendSegment(MEASURINGPOINT_REPOSITORY_FILENAME);
        Resource measuringpointResource = resourceSet.getResource(measuringpointPath, true);
        MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) measuringpointResource.getContents()
                .get(0);

        URI monitorPath = modelsPath.appendSegment(MONITOR_DIRECTORY_NAME).appendSegment(MONITOR_REPOSITORY_FILENAME);
        Resource monitorResource = resourceSet.getResource(monitorPath, true);
        MonitorRepository monitorRepo = (MonitorRepository) monitorResource.getContents().get(0);

        // Complete system model

        Resource systemResource = resourceSet.getResource(modelsPath.appendSegment(SYSTEM_MODEL_FILENAME), true);
        org.palladiosimulator.pcm.system.System system = (org.palladiosimulator.pcm.system.System) systemResource
                .getContents().get(0);

        List<AssemblyContext> systemAssemblies = system.getAssemblyContexts__ComposedStructure();
        AssemblyContext computeAssembly = (AssemblyContext) ModelConstructionUtils.findObjectWithId(systemAssemblies,
                COMPUTE_ASSEMBLY_CONTEXT_SYSTEM);

        // Add for later use
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
            
            // Create measuring point for each system operation
            
            SystemOperationMeasuringPoint systemOperationMp = PcmmeasuringpointFactory.eINSTANCE.createSystemOperationMeasuringPoint();
            systemOperationMp.setOperationSignature(this.jobSignatures.get(jobTypeName));
            systemOperationMp.setRole(role);
            systemOperationMp.setSystem(system);
            
            systemOperationMp.setMeasuringPointRepository(measuringPointRepo);
            
            // Create corresponding monitor
            
            // Find the original monitor from the monitoring repository
            List<Monitor> monitors = monitorRepo.getMonitors();
            Monitor blueprintSystemMonitor = ModelConstructionUtils.findObjectWithId(monitors, "systemOperationMonitor");

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

        // Complete Resource Environment

        Resource resourceEnvironmentResource = resourceSet
                .getResource(modelsPath.appendSegment(RESOURCE_ENVIRONMENT_MODEL_FILENAME), true);
        ResourceEnvironment resEnv = (ResourceEnvironment) resourceEnvironmentResource.getContents().get(0);

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

            // Set CPU properties
            cpuResourceSpec.setNumberOfReplicas(nodeType.getCores());

            PCMRandomVariable processingRate = CoreFactory.eINSTANCE.createPCMRandomVariable();
            processingRate.setSpecification(String.valueOf(nodeType.getComputingRate()));

            cpuResourceSpec.setProcessingRate_ProcessingResourceSpecification(processingRate);

            // Add new measuring points for the new resource
            addMeasuringpointsAndMonitors(measuringPointRepo, monitorRepo, cpuResourceSpec, blueprintCpuMonitor,
                    nodeTypeName);

            // Do not forget to change all IDs for the copied objects
            ModelConstructionUtils.changeIds(newNode);

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

            StereotypeAPI.applyStereotype(newNode, middlewareHostStereotype);
            StereotypeAPI.setTaggedValue(newNode, nodeType.getJobslots(), "MiddlewareHost", "capacity");
        }

        // Load and complete the Usage Model
        Resource usageModelResource = resourceSet.getResource(modelsPath.appendSegment(USAGE_MODEL_FILENAME), true);
        UsageModel usageModel = (UsageModel) usageModelResource.getContents().get(0);
        completeUsageModel(usageModel, jobs, blueprintResponseTimeMonitor, monitorRepo, measuringPointRepo);
        
        // Remove blueprint monitor from monitor repository
        blueprintResponseTimeMonitor.setMonitorRepository(null);

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
        
        // Save all resources and models
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

    public void loadParametersAndCompleteModels(final URI modelsPath, final URI parameterPath) {
        // final URI parameterPath, final IContainer target

        // Find parameter files and import data

        String nodeDescriptionPath = parameterPath.appendSegment(Config.NODE_DESCRIPTION_FILENAME).toString();
        File nodeDescriptionFile = FileHelper.getFile(nodeDescriptionPath);

        List<NodeTypeDescription> nodes = new ArrayList<>();
        try {
            nodes = ParameterJSONImportHelper.readNodeTypes(nodeDescriptionFile);
        } catch (Exception e) {
            System.out.println("Something went wrong when importing node types! E: " + e);
        }

        String jobTypeDescriptionPath = parameterPath.appendSegment(Config.JOB_DESCRIPTION_FILENAME).toString();
        File jobDescriptionFile = FileHelper.getFile(jobTypeDescriptionPath);
        List<JobTypeDescription> jobs = new ArrayList<>();
        try {
            jobs = ParameterJSONImportHelper.readJobTypes(jobDescriptionFile);
        } catch (Exception e) {
            System.out.println("Something went wrong when importing jobs types! E: " + e);
        }

        // Attempt to actually complete models
        completeModels(modelsPath, nodes, jobs);

    }

    public void completeRepositoryModel(Repository repository, List<JobTypeDescription> jobTypes) {

        List<RepositoryComponent> components = repository.getComponents__Repository();

        // TODO include more checks for correct model structure
        CompositeComponent computeJob = (CompositeComponent) ModelConstructionUtils.findObjectWithId(components,
                COMPUTE_JOB_COMPOSITE_COMPONENT_ID);

        BasicComponent blueprintJob = (BasicComponent) ModelConstructionUtils.findObjectWithId(components,
                BLUEPRINT_JOB_COMPONENT_ID);
        // System.out.println("Found basic Component: " +
        // EcoreUtil.getID(blueprintJob));

        // Find blueprint stereotypes

        List<Stereotype> blueprintStereotypes = StereotypeAPI.getAppliedStereotypes(blueprintJob);

        Stereotype middlewareDependencyStereotype = blueprintStereotypes.stream()
                .filter(stereotype -> "MiddlewareDependency".equals(stereotype.getName())).findAny().orElse(null);

        if (middlewareDependencyStereotype == null) {
            throw new IllegalArgumentException("Invalid model blueprint, missing middleware dependency stereotype!");
        }

        ServiceEffectSpecification seff = ModelConstructionUtils
                .findObjectWithId(blueprintJob.getServiceEffectSpecifications__BasicComponent(), BLUEPRINT_JOB_SEFF);

        // TODO Throw more meaningful exception to catch when calling this and notify
        // user about invalid model
        if (seff == null) {
            throw new IllegalArgumentException("Invalid model blueprint!");
        }

        ResourceDemandingSEFF resourceSeff = (ResourceDemandingSEFF) seff;

        // buildJobSEFF(resourceSeff, "test", jobTypes.get(0));

        for (JobTypeDescription job : jobTypes) {
            System.out.println("Adding job type to repository: " + job.getTypeName());
            buildAndAddJobComponentWithProvidedInterface(repository, job, resourceSeff, computeJob,
                    middlewareDependencyStereotype);
        }

        // BasicComponent copyJob = EcoreUtil.copy(blueprintJob);
        // System.out.println("ID of copy: " + EcoreUtil.getID(copyJob));
    }

    public void completeUsageModel(UsageModel usageModel, List<JobTypeDescription> jobs, Monitor responseTimeMonitor,
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

        // ScenarioBehaviour behaviour =
        // blueprintUsageScenario.getScenarioBehaviour_UsageScenario();
        // List<AbstractUserAction> actions = behaviour.getActions_ScenarioBehaviour();
        // Branch jobtypeBranch = findObjectWithId(actions,
        // BLUEPRINT_USAGEMODEL_BRANCH_JOBTYPE);

        // Blueprint only has one branch transition
        BranchTransition blueprintTransition = null;
        List<BranchTransition> blueprintTransitionList = jobtypeBranch.getBranchTransitions_Branch();

        if (blueprintTransitionList.size() < 1) {
            throwNewInvalidModelException("Usage Model", "Could not find branch transition.");
        }
        blueprintTransition = blueprintTransitionList.get(0);

        for (JobTypeDescription jobType : jobs) {
            String jobTypeName = jobType.getTypeName();

            BranchTransition newTransition = EcoreUtil.copy(blueprintTransition);

            // Set correct branch probability
            // TODO Maybe normalize the values here again in case of invalid JSON data?
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
            
            // For now, do not add system call monitor and measuring points
            // addSystemCallMonitor(measuringpointRepo, monitorRepo, systemCall, responseTimeMonitor, jobTypeName);
            
            // In the end, change all IDs for the new branch transition
            ModelConstructionUtils.appendIDsRecursively(newTransition, jobTypeName);

            newTransition.setBranch_BranchTransition(jobtypeBranch);
        }

        // Remove blueprint branch transition from the usage model.
        // This is important to not alter the generated load profile.
        blueprintTransition.setBranch_BranchTransition(null);
    }

    public void addMeasuringpointsAndMonitors(MeasuringPointRepository measuringPointRepo,
            MonitorRepository monitorRepo, ProcessingResourceSpecification processingSpec, Monitor originalMonitor,
            String additionalSuffix) {

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

            // TODO Do not activate all monitors?
            duplicatedMonitor.setActivated(true);
        }

    }

    public void addSystemCallMonitor(MeasuringPointRepository measuringPointRepo, MonitorRepository monitorRepo,
            EntryLevelSystemCall entryCall, Monitor originalMonitor, String additionalSuffix) {

        EntryLevelSystemCallMeasuringPoint point = PcmmeasuringpointFactory.eINSTANCE
                .createEntryLevelSystemCallMeasuringPoint();
        point.setEntryLevelSystemCall(entryCall);

        Monitor duplicatedMonitor = ModelConstructionUtils.copyAppendIds(originalMonitor, additionalSuffix);

        String monitorName = MessageFormat.format("Response Time Monitor EntryLevelSystemCall {0}", additionalSuffix);
        duplicatedMonitor.setEntityName(monitorName);

        addMonitorToModel(measuringPointRepo, monitorRepo, point, duplicatedMonitor);
    }

    public void addMonitorToModel(MeasuringPointRepository measuringPointRepo, MonitorRepository monitorRepo,
            MeasuringPoint measuringPoint, Monitor monitor) {

        measuringPoint.setMeasuringPointRepository(measuringPointRepo);

        monitor.setMeasuringPoint(measuringPoint);
        monitor.setMonitorRepository(monitorRepo);
        monitor.setActivated(true);
    }

    public BasicComponent buildAndAddJobComponentWithProvidedInterface(Repository repository,
            JobTypeDescription jobType, ResourceDemandingSEFF blueprintSeff, CompositeComponent computeJob,
            Stereotype stereotypeToApply) {

        BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();

        String jobTypeName = jobType.getTypeName();

        component.setEntityName(jobTypeName);
        repository.getComponents__Repository().add(component);

        StereotypeAPI.applyStereotype(component, stereotypeToApply);

        StereotypeAPI.setTaggedValue(component, jobType.getRequiredJobslotsStoEx(), "MiddlewareDependency",
                "numberRequiredResources");

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

        // ResourceDemandingSEFF seff = buildJobSEFF(blueprintSeff, jobTypeName,
        // jobType);
        // seff.setDescribedService__SEFF(jobInterfaceSignature);

        ResourceDemandingSEFF seff = EcoreUtil.copy(blueprintSeff);

        ForkAction forkAction = null;
        EObject forkObject = ModelConstructionUtils.findObjectWithIdRecursively(seff, BLUEPRINT_JOB_FORK);
        if (forkObject instanceof ForkAction) {
            forkAction = (ForkAction) forkObject;
        }

        if (forkAction == null) {
            throwNewInvalidModelException("Repository", "Could not find fork action in job RDSEFF.");
        }

        ModelConstructionUtils.appendIDsRecursively(seff, "_" + jobTypeName);

        AbstractAction predecessorAction = forkAction.getPredecessor_AbstractAction();
        AbstractAction successorAction = forkAction.getSuccessor_AbstractAction();

        // Remove fork from SEFF
        forkAction.setResourceDemandingBehaviour_AbstractAction(null);

        // TODO Factor out strings
        BranchAction branch = duplicateBehaviours(forkAction, "NUMBER_REQUIRED_RESOURCES", 8);
        branch.setResourceDemandingBehaviour_AbstractAction(seff);

        branch.setPredecessor_AbstractAction(predecessorAction);
        branch.setSuccessor_AbstractAction(successorAction);

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

        VariableUsage resourceDemandRounds = ModelConstructionUtils
                .createVariableUsageWithValue("RESOURCE_DEMAND_ROUNDS", Integer.toString(10));
        component.getComponentParameterUsage_ImplementationComponentType().add(resourceDemandRounds);

        // Add component to repository
        repository.getComponents__Repository().add(component);

        // Add component to computing job component
        AssemblyContext assembly = CompositionFactory.eINSTANCE.createAssemblyContext();
        assembly.setEncapsulatedComponent__AssemblyContext(component);
        assembly.setParentStructure__AssemblyContext(computeJob);
        
        assembly.setEntityName("assembly_context_" + jobTypeName);

        // Add dependent parameter usage
        VariableUsage ioVariableUsage = ModelConstructionUtils.createVariableUsageWithValue("IO_DEMAND",
                "IO_RATIO.VALUE * CPU_DEMAND.VALUE");
        assembly.getConfigParameterUsages__AssemblyContext().add((ioVariableUsage));

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

        // Copy object, change IDs below after finding contained objects from blueprint
        ResourceDemandingSEFF newSeff = EcoreUtil.copy(blueprintSEFF);

        InternalAction cpuDemandAction = null;
        InternalAction ioDemandAction = null;

        EObject actionObject = ModelConstructionUtils.findObjectWithIdRecursively(newSeff, BLUEPRINT_JOB_CPU_ACTION);
        if (actionObject instanceof InternalAction) {
            cpuDemandAction = (InternalAction) actionObject;
        }

        actionObject = ModelConstructionUtils.findObjectWithIdRecursively(newSeff, BLUEPRINT_JOB_IO_ACTION);
        if (actionObject instanceof InternalAction) {
            ioDemandAction = (InternalAction) actionObject;
        }

        // Iterate through the actions to find the random variable for I/O, CPU demand
        ParametricResourceDemand cpuDemand = findParametricResourceDemand(cpuDemandAction);
        ParametricResourceDemand ioDemand = findParametricResourceDemand(ioDemandAction);

        if (cpuDemand == null || ioDemand == null) {
            throw new IllegalArgumentException("Invalid model blueprint: missing internal actions.");
        }

        PCMRandomVariable cpuVar = CoreFactory.eINSTANCE.createPCMRandomVariable();
        cpuVar.setSpecification(jobType.getCpuDemandStoEx());
        cpuDemand.setSpecification_ParametericResourceDemand(cpuVar);

        PCMRandomVariable ioVar = CoreFactory.eINSTANCE.createPCMRandomVariable();
        ioVar.setSpecification(jobType.getIoTimeStoEx());
        ioDemand.setSpecification_ParametericResourceDemand(ioVar);

        cpuDemand.setSpecification_ParametericResourceDemand(cpuVar);
        ioDemand.setSpecification_ParametericResourceDemand(ioVar);

        // Change IDs to prevent conflicts
        ModelConstructionUtils.changeIds(newSeff);

        return newSeff;
    }

    public static BranchAction duplicateBehaviours(ForkAction containingAction, String duplicationCountParameterName,
            int maxThreads) {

        ForkAction localContainingAction = ModelConstructionUtils.copyChangeIds(containingAction);

        BranchAction branchResult = SeffFactory.eINSTANCE.createBranchAction();

        for (int i = 1; i <= maxThreads; i++) {
            ForkAction newFork = ModelConstructionUtils.copyAppendIds(localContainingAction, "_threadcount_" + i);

            SynchronisationPoint syncPoint = newFork.getSynchronisingBehaviours_ForkAction();
            if (syncPoint == null) {
                throw new IllegalArgumentException("ForkAction must have synchronized behaviours.");
            }

            // TODO Check for sanity of the containing action.
            List<ForkedBehaviour> behaviourList = syncPoint.getSynchronousForkedBehaviours_SynchronisationPoint();
            if (behaviourList.size() < 1) {
                throw new IllegalArgumentException("No synchronized behaviours found.");
            }

            ForkedBehaviour blueprintBehaviour = behaviourList.get(0);

            for (int j = 2; j <= i; j++) {
                ForkedBehaviour newBehaviour = ModelConstructionUtils.copyAppendIds(blueprintBehaviour, "_thread_" + j);
                newBehaviour.setSynchronisationPoint_ForkedBehaviour(syncPoint);
            }

            PCMRandomVariable branchCondition = CoreFactory.eINSTANCE.createPCMRandomVariable();
            branchCondition.setSpecification(duplicationCountParameterName + ".VALUE == " + i);

            GuardedBranchTransition transition = SeffFactory.eINSTANCE.createGuardedBranchTransition();
            transition.setBranchCondition_GuardedBranchTransition(branchCondition);
            transition.setEntityName("transition_run_with_" + i + "cores");

            // Create behaviour for inside of the branched paths
            ResourceDemandingBehaviour behaviour = SeffFactory.eINSTANCE.createResourceDemandingBehaviour();

            StartAction start = SeffFactory.eINSTANCE.createStartAction();
            start.setEntityName("startAction in Branch Transition, core count:" + i);
            StopAction stop = SeffFactory.eINSTANCE.createStopAction();

            transition.setBranchBehaviour_BranchTransition(behaviour);
            transition.setBranchAction_AbstractBranchTransition(branchResult);

            // Add actions to behaviour
            start.setResourceDemandingBehaviour_AbstractAction(behaviour);
            newFork.setResourceDemandingBehaviour_AbstractAction(behaviour);
            stop.setResourceDemandingBehaviour_AbstractAction(behaviour);

            // Create correct behaviour control flow
            start.setSuccessor_AbstractAction(newFork);

            newFork.setPredecessor_AbstractAction(start);
            newFork.setSuccessor_AbstractAction(stop);

            stop.setPredecessor_AbstractAction(newFork);
        }

        return branchResult;
    }

    private ParametricResourceDemand findParametricResourceDemand(EObject object) {
        if (object == null) {
            return null;
        }

        TreeIterator<EObject> j = object.eAllContents();
        while (j.hasNext()) {
            EObject obj = j.next();

            if (obj instanceof ParametricResourceDemand) {
                return (ParametricResourceDemand) obj;
            }
        }

        return null;

    }

    private void throwNewInvalidModelException(String affectedModel, String message) {
        String failureMessage = MessageFormat.format("Invalid {0} model: {1}", affectedModel, message);
        throw new IllegalArgumentException(failureMessage);
    }
}
