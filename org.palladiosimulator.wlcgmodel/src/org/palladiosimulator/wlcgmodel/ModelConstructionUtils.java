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

import de.uka.ipd.sdq.stoex.StoexFactory;
import de.uka.ipd.sdq.stoex.VariableReference;

public class ModelConstructionUtils {

    public static VariableUsage createVariableUsageWithValue(String parameterName, String valueSpecification) {

        VariableUsage result = ParameterFactory.eINSTANCE.createVariableUsage();

        VariableReference varReference = StoexFactory.eINSTANCE.createVariableReference();
        varReference.setReferenceName(parameterName);

        result.setNamedReference__VariableUsage(varReference);

        VariableCharacterisation characterization = ParameterFactory.eINSTANCE.createVariableCharacterisation();

        characterization.setType(VariableCharacterisationType.VALUE);
        characterization.setVariableUsage_VariableCharacterisation(result);

        PCMRandomVariable randomVariable = CoreFactory.eINSTANCE.createPCMRandomVariable();
        randomVariable.setSpecification(valueSpecification);

        characterization.setSpecification_VariableCharacterisation(randomVariable);

        return result;
    }

    /**
     * Find the object with known ID in the list, return null if there is no such
     * object.
     * 
     * @param objects
     * @param id
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
     * Find the object with know ID in all objects contained in the passed object,
     * null if there is no such object.
     * 
     * @param object
     * @param id
     * @return
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

    public static <T extends EObject> T copyChangeIds(T object) {

        // TODO This should return a deep copy including all containment references, is
        // this accurate?
        // TODO Maybe try this in a smaller setting to check?
        T result = EcoreUtil.copy(object);
        EcoreUtil.setID(result, EcoreUtil.generateUUID());

        TreeIterator<EObject> i = result.eAllContents();
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

}
