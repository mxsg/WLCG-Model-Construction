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
import org.palladiosimulator.pcm.seff.seff_performance.ParametricResourceDemand;

import de.uka.ipd.sdq.stoex.StoexFactory;
import de.uka.ipd.sdq.stoex.VariableReference;

/**
 * This utility class contains methods that simplify the construction of the calibrated Palladio model.
 *
 * @author Maximilian Stemmer-Grabow
 */
public class ModelConstructionUtils {

    /**
     * Create and return a value variable usage description with given name and specification.
     *
     * @param parameterName The name of the variable to be described.
     * @param valueSpecification The specification for the variable.
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
     * Find the object with known ID in the list, return null if there is no such object.
     *
     * @param objects List of objects to be searched for the object with given ID.
     * @param id The ID of the object to be searched.
     * @param <T> The type of the object to be searched for, which has to be an Ecore object.
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
     * Find the object with know ID in all objects contained in the passed object, null if there is
     * no such object.
     *
     * @param object The object to be recursively searched.
     * @param id The ID of the object to be searched for.
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
     * Make a deep copy of the provided objects and change all IDs of objects contained in it
     * via a containment reference.
     *
     * @param object The Ecore object to be copied.
     * @param <T> The type of the object to be searched for, which has to be an Ecore object.
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

    public static <T extends EObject> T copyAppendIds(T object, String suffix) {

        if (object == null) {
            return null;
        }

        T result = EcoreUtil.copy(object);
        appendIDsRecursively(result, suffix);
        return result;
    }

    public static void changeIds(EObject object) {
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

    public static void appendIDsRecursively(EObject object, String suffix) {

        if (object == null) {
            return;
        }

        appendToID(object, suffix);

        TreeIterator<EObject> i = object.eAllContents();
        while (i.hasNext()) {
            EObject obj = i.next();
            appendToID(obj, suffix);
        }
    }

    /**
     * Find and return the parametric resource demand that is contained in the supplied Ecore object.
     *
     * @param object The Ecore object to be searched in.
     * @return The first parametric resource demand that could be found in a containment reference of the
     * supplied object, or null if no such object could be found.
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

}
