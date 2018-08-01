package org.palladiosimulator.wlcgmodel;

import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.parameter.ParameterFactory;
import org.palladiosimulator.pcm.parameter.VariableCharacterisation;
import org.palladiosimulator.pcm.parameter.VariableCharacterisationType;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.seff.BranchAction;
import org.palladiosimulator.pcm.seff.ForkAction;
import org.palladiosimulator.pcm.seff.ForkedBehaviour;
import org.palladiosimulator.pcm.seff.GuardedBranchTransition;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.seff.SynchronisationPoint;
import org.palladiosimulator.pcm.seff.seff_performance.ParametricResourceDemand;

import de.uka.ipd.sdq.stoex.StoexFactory;
import de.uka.ipd.sdq.stoex.VariableReference;

/**
 * This utility class contains methods that simplify the construction of a calibrated performance
 * model.
 *
 * @author Maximilian Stemmer-Grabow
 */
public class ModelConstructionUtils {

    /**
     * Create and return a value variable usage description with given name and specification.
     *
     * @param parameterName
     *            The name of the variable to be described.
     * @param valueSpecification
     *            The specification for the variable.
     * @return A variable usage description that describes the value of the provided variable.
     */
    public static VariableUsage createVariableUsageWithValue(String parameterName, String valueSpecification) {

        final VariableCharacterisationType characterisationType = VariableCharacterisationType.VALUE;

        VariableUsage result = ParameterFactory.eINSTANCE.createVariableUsage();

        VariableReference varReference = StoexFactory.eINSTANCE.createVariableReference();
        varReference.setReferenceName(parameterName);

        result.setNamedReference__VariableUsage(varReference);

        VariableCharacterisation characterization = ParameterFactory.eINSTANCE.createVariableCharacterisation();

        characterization.setType(characterisationType);
        characterization.setVariableUsage_VariableCharacterisation(result);

        PCMRandomVariable randomVariable = CoreFactory.eINSTANCE.createPCMRandomVariable();
        randomVariable.setSpecification(valueSpecification);

        characterization.setSpecification_VariableCharacterisation(randomVariable);

        return result;
    }

    /**
     * Find the object with known ID in the list and return it, return null if there is no such
     * object.
     *
     * @param objects
     *            List of objects to be searched for the object with given ID.
     * @param id
     *            The ID of the object to be searched.
     * @param <T>
     *            The type of the object to be searched for, which has to be an Ecore object.
     * @return The found object, or null if there is no such object.
     */
    public static <T extends EObject> T findObjectWithId(List<T> objects, String id) {
        try {
            return objects.stream().filter(obj -> EcoreUtil.getID(obj).contentEquals(id)).findAny().get();
        } catch (NoSuchElementException e) {
            // Could not find matching element
            return null;
        }
    }

    /**
     * Find the object with know ID in all objects contained in the passed object, or null if there
     * is no such object.
     *
     * @param object
     *            The object to be recursively searched.
     * @param id
     *            The ID of the object to be searched for.
     * @return The found object, or null if there is no such object.
     */
    public static EObject findObjectWithIdRecursively(EObject object, String id) {

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

    /**
     * Reset the ID of an Ecore object to a new random UUID provided by the Ecore framework. Ignores
     * objects that do not have an ID associated with them.
     *
     * @param object
     *            The object whose ID should be changed.
     */
    public static void setRandomID(EObject object) {
        try {
            EcoreUtil.setID(object, EcoreUtil.generateUUID());
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException is thrown in case this method is called with an Ecore object
            // that does not have an ID. In this case, simply skip the object.
        }
    }

    /**
     * Change the ID of an Ecore object by appending the provided string. Ignores objects that do
     * not have an ID associated with them.
     *
     * @param object
     *            The object whose ID should be changed.
     * @param suffix
     *            The suffix to append to the old ID.
     */
    public static void appendToID(EObject object, String suffix) {

        String originalID = EcoreUtil.getID(object);

        // If the original object had no ID, do nothing
        if (originalID == null) {
            return;
        }

        if (suffix == null) {
            suffix = "";
        }

        EcoreUtil.setID(object, originalID + suffix);
    }

    /**
     * Reset the ID of an Ecore object and all IDs of object contained in it via a containment
     * reference to a new random UUID provided by the Ecore framework. Ignores objects that do not
     * have an ID associated with them.
     *
     * @param object
     *            The object whose contained objects should be traversed and whose IDs should be
     *            changed. If null, do nothing.
     */
    public static void setRandomIDsRecursively(EObject object) {
        if (object == null) {
            return;
        }

        // Change the ID of the object itself
        setRandomID(object);

        TreeIterator<EObject> i = object.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();
            setRandomID(obj);
        }
    }

    /**
     * Set the ID of an Ecore object and all IDs of object contained in it via a containment
     * reference by appending a string. Ignores objects that do not have an ID associated with them.
     *
     * @param object
     *            The object whose contained objects should be traversed and whose IDs should be
     *            changed. If null, do nothing.
     * @param suffix
     *            The suffix to be appended to the IDs in the object tree.
     */
    public static void appendIDsRecursively(EObject object, String suffix) {
        if (object == null) {
            return;
        }

        // Change the ID of the object itself
        appendToID(object, suffix);

        TreeIterator<EObject> i = object.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();
            appendToID(obj, suffix);
        }
    }

    /**
     * Make a deep copy of the provided objects and change all IDs of objects contained in it via a
     * containment reference.
     *
     * @param object
     *            The Ecore object to be copied.
     * @param <T>
     *            The type of the object to be searched for, which has to be an Ecore object.
     * @return A deep copy of the object where all IDs have been changed.
     */
    public static <T extends EObject> T copyChangeIds(T object) {

        // This returns a deep copy including all containment references
        T result = EcoreUtil.copy(object);

        // Reset ID of the top-level object
        EcoreUtil.setID(result, EcoreUtil.generateUUID());

        TreeIterator<EObject> i = result.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();

            // Reset all IDs for contained objects that have IDs
            try {
                EcoreUtil.setID(obj, EcoreUtil.generateUUID());
            } catch (IllegalArgumentException e) {
                // Object does not have ID, do not reset anything
            }
        }
        return result;
    }

    /**
     * Make a deep copy of an Ecore object (copy including copies of object included in containment
     * references), and append a suffix to each ID in the copied object tree.
     *
     * @param object
     *            The object to be copied. If null, nothing is copied and null returned.
     * @param suffix
     *            The String suffix to add to each ID in the object tree.
     * @param <T>
     *            The type of the object. Has to be an Ecore object.
     * @return A deep copy of the provided object.
     */
    public static <T extends EObject> T copyAppendIds(T object, String suffix) {

        if (object == null) {
            return null;
        }

        T result = EcoreUtil.copy(object);
        appendIDsRecursively(result, suffix);
        return result;
    }

    /**
     * Find and return the parametric resource demand that is contained in the supplied Ecore
     * object.
     *
     * @param object
     *            The Ecore object to be searched in.
     * @return The first parametric resource demand that could be found in a containment reference
     *         of the supplied object, or null if no such object could be found.
     */
    public static ParametricResourceDemand findParametricResourceDemand(EObject object) {
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

    /**
     * Duplicate the contents of a fork action according to a parameter whose VALUE variable
     * characterisation describes the number of threads the action inside of the fork should be
     * split up into.
     *
     * @param containingAction
     *            The forked action containing the behaviour to be duplicated.
     * @param duplicationCountParameterName
     *            The name of the variable that is used to choose the correct number of threads the
     *            action is to be split into.
     * @param maxThreads
     *            The maximum number of forks the action should be able to be split into.
     * @return A branch action that contains the fork actions and a branch condition that selects
     *         the correct fork action based on the duplication count parameter.
     */
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

            // TODO Check for validity of the containing action
            List<ForkedBehaviour> behaviourList = syncPoint.getSynchronousForkedBehaviours_SynchronisationPoint();
            if (behaviourList.size() < 1) {
                throw new IllegalArgumentException("No synchronized behaviours found.");
            }

            ForkedBehaviour blueprintBehaviour = behaviourList.get(0);

            for (int j = 2; j <= i; j++) {
                ForkedBehaviour newBehaviour = ModelConstructionUtils.copyAppendIds(blueprintBehaviour, "_thread_" + j);
                newBehaviour.setSynchronisationPoint_ForkedBehaviour(syncPoint);
            }

            // Set branch condition to branch according to the number of threads to create
            PCMRandomVariable branchCondition = CoreFactory.eINSTANCE.createPCMRandomVariable();
            branchCondition.setSpecification(duplicationCountParameterName + ".VALUE == " + i);

            GuardedBranchTransition transition = SeffFactory.eINSTANCE.createGuardedBranchTransition();
            transition.setBranchCondition_GuardedBranchTransition(branchCondition);
            transition.setEntityName("transition_run_with_" + i + "_cores");

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

}
