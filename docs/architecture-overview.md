# Architecture

This plugin is based on the concept of creating a parametric Palladio model by constructing it from

- a **blueprint model** (also included in [this project](../org.palladiosimulator.wlcgmodel/blueprint-wlcg))
- and **parameter description files** (example files are also [included](../org.palladiosimulator.wlcgmodel/parameters)). Their structure and requirements are described [here](calibration-parameter-files.md).

## Notes

- The `WLCGModelConstructor` class in the `org.palladiosimulator.wlcgmodel` package contains the main functionality of building the complete simulation model from the blueprint model.
- The model construction process is structured according to the blueprint model's structure itself, so it may serve as a reference.

## Project Structure

Source code structure:

- `org.palladiosimulator.wlcgmodel`
    - This package contains the main functionality of the plugin.
    - Functionality to load parameter files, import a blueprint model and complete it is provided by the `BlueprintModelImport` class. 
    - Actual functionality related to construction of the simulation model is located in `WLCGModelConstructor`.
    - `JobTypeDescription` and `NodeTypeDescription` hold the model calibration parameters and are loaded from JSON source files by `ParameterJSONImportHelper` using Gson.
    - `Config` holds plugin-wide configuration information.

- `org.palladiosimulator.wlcgmodel.util`
    - Contains convenience/utility functionality related to project creation

- `org.palladiosimulator.wlcgmodel.commands`
    - Contains menu action handling code

- `org.palladiosimulator.wlcgmodel.wizards`
    - Contains project wizard handling code


Additional resources in this repository:

- `blueprint-wlcg` contains the blueprint Palladio model used for model construction
- `parameters` contains a sample parameter set for calibration

## Project Eclipse Extensions

The project provides multiple extensions to Eclipse to expose its functionality:

- `org.eclipse.ui.newWizards`: A new project creation wizard
- `org.eclipse.ui.commands`: A menu item handling command
- `org.eclipse.ui.menu`: Context menu entries
