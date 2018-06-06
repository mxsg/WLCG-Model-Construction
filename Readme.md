# WLCG Model Completion

This Eclipse plugin allows to create WLCG Palladio models from JSON parameter description files.

## Usage

Installation:

- This plugin requires the [Gson Java (De)serialization](https://github.com/google/gson) package, which is currently not included in the plugin
    - Download the package jar file in version 2.8.5 from [here](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.5/gson-2.8.5.jar) (direct download link)
    - Place it in the `lib` directory at the plugin root
    - The external jar is already included in the project and plugin classpath

Notes:

- WLCG model files can be created in any Palladio modeling project. Be aware that in case of conflicts, the plugin will overwrite conflicting model files.
- To create a WLCG model, JSON parameter description files are needed
    - Place them in a directory in your modeling project
    - The files are expected to be named `nodes.json` and `jobs.json`

Usage:

1. Right-click on the directory containing your parameter description files in the *model explorer*, *package explorer* or *project explorer* view
2. Click "Complete WLCG Model with this parameter set"
3. Confirm that you want to overwrite conflicting model files
4. A Dialog will indicate completion of the model construction or show an error if it failed
