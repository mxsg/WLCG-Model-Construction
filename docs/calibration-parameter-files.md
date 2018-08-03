# Calibration Parameter Files

Models are constructed using parameter description files in JSON format. Two separate files are required:

- a file containing job type and resource demand descriptions
- a file containing node type and performance information (resource environment description)

Examples of these description files are [included with the plugin](../org.palladiosimulator.wlcgmodel/parameters).

## Job Description Files

This file contains a top level **list** of job types. Each job type in this list is described by a **dictionary** with the following structure (shortened example data):

```
{
    "typeName" : "analysis",
	"cpuDemandStoEx" : "DoublePDF[...]",
	"ioTimeStoEx" : "DoublePDF[...]",
	"ioTimeRatioStoEx" : "DoublePDF[...]",
	"requiredJobslotsStoEx" : "IntPMF[...]",
	"relativeFrequency" : 0.23876534
}
```

The keys have the following meaning:

- `typeName` is a name for the job type to be referred to as in the model.
- `cpuDemandStoEx` is a Palladio stochastic expression for the total CPU demand a job from this type requires. It can be given in any arbitrary unit, but needs to be consistent with the `computingRate` property of the node description files.
- `ioTimeStoEx` is a stochastic expression for the total I/O time a job from this type requires.
- `ioTimeRatioStoEx` is a stochastic expression for the distribution of the ratio of the job that is used for I/O when compared to the CPU demand.
- `requiredJobslotsStoEx` is a stochastic expression (e.g. integer mass distribution function) for the number of cores/jobslots a job of this type requires.
- `relativeFrequency` is the frequency with which jobs of this type occurs. Over all job types included in the calibration parameter file, this parameter should sum up to unity.


## Node Description Files

This file contains a top level **list** of node types. Each node type in this list is described by a **dictionary** with the following structure (example data):

```
{
    "computingRate": 12.1875,
    "cores": 20,
    "jobslots": 32,
    "name": "Intel(R) Xeon(R) CPU E5-2630 v4 @ 2.20GHz",
    "nodeCount": 138
}
```

The keys have the following meaning:

- `computingRate` is the computing rate of a single core of the node of this type. It can be given in any arbitrary unit, but needs to be consistent with the `cpuDemandStoEx` property of the node description files.
- `cores` is the number of cores (duplicated CPUs) a single node of this type has.
- `jobslots` is the number of slots a node of this type is configured for, i.e. the number of single core jobs are allowed to be run on this machine concurrently.
- `name` is a name of the node type to be reffered to as in the model.
- `nodeCount` is the number of identical nodes with these properties to be included in the simulation model.
