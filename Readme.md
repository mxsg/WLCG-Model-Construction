# WLCG Model Construction

This Eclipse plugin allows to create WLCG Palladio models from JSON model parameter description files.

## Notes

- WLCG model files can be created in any Palladio modeling project. Be aware that in case of conflicts, the plugin will overwrite conflicting model files.
- To create a WLCG model, JSON parameter description files are needed
    - Place them in a directory in your modeling project
    - The files are expected to be named `nodes.json` and `jobs.json`


## Usage

1. Right-click on the directory containing your parameter description files in the *model explorer*, *package explorer* or *project explorer* view
2. Click "Complete WLCG Model with this parameter set"
3. Confirm that you want to overwrite conflicting model files
4. A Dialog will indicate completion of the model construction or show an error if it failed
