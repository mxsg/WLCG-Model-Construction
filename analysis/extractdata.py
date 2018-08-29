#!/usr/bin/env python3

# In case this code is run with Python 2
from __future__ import division

import datetime
import logging
import os
import re
import sys

import pandas as pd


def simulation_stop_time(csv_path):
    df = pd.read_csv(csv_path, sep=',')

    df.columns = ['time_start', 'state']

    # The start of the last state indicates the end of the simulation
    return df.iloc[-1].time_start


def calculate_utilization(csv_path):
    df = pd.read_csv(csv_path, sep=',')

    df.columns = ['time_start', 'state']
    df['time_end'] = df.time_start.shift(-1)
    df['slice_length'] = df.time_end - df.time_start

    total_time = df.iloc[-1].time_start - df.iloc[0].time_start

    # Drop last row with end timestamp
    df = df[:-1]

    busy_time = df[df.state >= 1].slice_length.sum()
    utilization = busy_time / total_time
    # print("Busy time: {}, total length: {}, utilization: {}".format(busy_time, total_time, utilization))

    return utilization


def single_utilization_timeseries(csv_path, identifier=None):
    """Return a time series that includes the time stamps of changes in utilization of a single core.

    Expects a csv in the format of <Timestamp>,<State> where <State> is the number of threads concurrently using the
    resource.
    """
    df = pd.read_csv(csv_path, sep=',')

    df.columns = ['time_start', 'state']

    # Check whether the time series has duplicates
    duplicate_count = df['time_start'].duplicated().sum()
    if duplicate_count > 0:
        print("Utilization time series has {} duplicates!".format(duplicate_count))

        # Assume that in case of duplicates, the last entry is the correct one
        # that applies to the following time span.
        df = df.drop_duplicates('time_start', keep='last')

    # Set index to timestamp
    df = df.set_index('time_start')

    # state > 1 indicates multiple threads using the resource,
    # hence clamp the values to 1 or below in this case
    df['utilized'] = df['state'].clip(upper=1)

    # Compute the changes in state by computing the delta cores utilized at the specific time
    # diff computes y[i] = x[i] - x[i-1]
    df['delta_state'] = df['utilized'].diff()

    # Fill in first row with original value for initial state, either 0 or 1
    df['delta_state'].iloc[0] = df['utilized'].iloc[0]

    # Add identifier column if indicated in parameters
    if identifier is not None:
        df['identifier'] = identifier

    return df


def multicore_utilization_timeseries(dfs, delta_state_col='delta_state', busy_core_col='busy_cores'):
    """Aggregate multiple utilization time series into an overall utilization (number of busy cores)."""

    df = pd.concat(dfs)

    df = df.sort_index()
    df = df[[delta_state_col]]

    # Remove duplicated time_stamps by summing over the deltas
    df = df.groupby(df.index).sum()

    # Compute the number of cores that are busy
    df[busy_core_col] = df[delta_state_col].cumsum()

    return df


def average_core_utilization(paths):
    utilizations = [calculate_utilization(path) for path in paths]
    avg_utilization = sum(utilizations) / len(utilizations) if len(utilizations) > 0 else 0.0

    return avg_utilization


def count_lines(path, headerlen=0):
    return sum(1 for _ in open(path)) - headerlen


def job_type_throughputs(filenames, directory):
    type_regex = r'jobsSystem.provided_role_system_(.*?)([0-9].*?)?\.run_(.*)\.csv'
    result = []

    pattern = re.compile(type_regex)
    print(filenames)
    job_types = [(pattern.findall(filename)[0][0], filename) for filename in filenames]

    for type, path in job_types:
        job_throughput = count_lines(os.path.join(directory, path), headerlen=1)
        result.append((type, job_throughput))

    return result


def job_type_response_times(filenames, directory):
    type_regex = r'ExternalCall_externalCallType_(.*?)([0-9].*?)?_(.*)\.csv'

    pattern = re.compile(type_regex)

    # Contains tuples with the name of the job type and the file name
    job_types = [(pattern.findall(filename)[0][0], filename) for filename in filenames]

    dfs = []

    for type_name, filename in job_types:
        path = os.path.join(directory, filename)

        df = pd.read_csv(path, sep=',')
        df.columns = ['time', 'response_time']
        df['type'] = type_name

        dfs.append(df)

    result = pd.concat(dfs)
    return result


def passive_resource_utilization(csv_path, stop_time):
    df = pd.read_csv(csv_path, sep=',')

    df.columns = ['time_start', 'available_slots']

    maxslots = df['available_slots'].max()

    df = df.groupby('time_start').last().reset_index()

    df['time_end'] = df.time_start.shift(-1)

    # Set end time of last row
    df.at[df.index[-1], 'time_end'] = stop_time

    df['slice_length'] = df.time_end - df.time_start

    total_time = df.iloc[-1].time_end - df.iloc[0].time_start
    average_available_slots = df['slice_length'].dot(df['available_slots']) / total_time

    return average_available_slots, maxslots


def total_passive_resource_utilization(paths, stop_time):
    avg_max_slots = [passive_resource_utilization(path, stop_time) for path in paths]

    slot_lists = [list(x) for x in zip(*avg_max_slots)]
    avg_slots = slot_lists[0]
    max_slots = slot_lists[1]

    avg_available_slots = sum(avg_slots)
    total_slots = sum(max_slots)

    return total_slots - avg_available_slots, total_slots


def main():
    if len(sys.argv) != 2:
        print("Usage: python utilization.py <directory>")
        print("<directory> directory look for CSV utilization files in")
        sys.exit(1)

    directory = sys.argv[1]
    print("Output directory: {}".format(directory))
    output_file = 'extraction_results.txt'
    output_dir = 'results'
    os.makedirs(os.path.join(directory, output_dir), exist_ok=True)

    # Logging setup
    logging.basicConfig(
        format='%(message)s',
        # format='%(asctime)s [%(levelname)-5.5s]  %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.FileHandler(os.path.join(directory, output_file)),
            logging.StreamHandler(stream=sys.stdout)
        ],
        level=logging.DEBUG)

    logging.info("# Analysis run at {}".format(datetime.datetime.now()))
    logging.info("Results stored in file {} (in input directory).".format(output_file))
    logging.info("")

    utilization_keyword = 'State_of_Active_Resource_Tuple'
    overall_throughput_keyword = 'Usage_Scenario'
    type_throughput_keyword = 'provided_role_system'
    passive_resource_keyword = 'State_of_Passive_Resource_Tuple'
    external_call_keyword = 'ExternalCall'

    # Compute core utilization
    logging.info("## Core Utilizations")

    all_files = [os.path.join(directory, i) for i in os.listdir(directory) if
                 os.path.isfile(os.path.join(directory, i))]

    utilization_file_paths = [path for path in all_files if utilization_keyword in path]

    logging.info("Number of utilization paths: {}".format(len(utilization_file_paths)))
    core_count = len(utilization_file_paths)

    # Create a plot of the utilization over time
    utilization_dfs = [single_utilization_timeseries(path) for path in utilization_file_paths]
    utilization_timeseries = multicore_utilization_timeseries(utilization_dfs)

    utilization_timeseries['utilization'] = utilization_timeseries['busy_cores'] / core_count

    # Compute throughput
    logging.info("")
    logging.info("## Throughput")

    total_throughput_paths = [path for path in all_files if overall_throughput_keyword in path]

    if not total_throughput_paths:
        sys.exit("Could not find total utilization file.")

    total_throughput = count_lines(total_throughput_paths[0], headerlen=1)
    logging.info("Total throughput (Usage Scenario): {}".format(total_throughput))

    # Compute throughput for single job types
    logging.info("")
    logging.info("## Throughput Per Type")

    job_throughput_filenames = [i for i in os.listdir(directory) if
                                os.path.isfile(os.path.join(directory, i)) and type_throughput_keyword in i]

    # Sort job types by their name
    throughputs = job_type_throughputs(job_throughput_filenames, directory)
    throughputs.sort(key=lambda x: x[0])

    # Compute time max length
    stoptime = simulation_stop_time(utilization_file_paths[0])

    # Compute number of days from the maximum time in the simulation (86400 seconds in a day)
    simulation_days = stoptime / 86400

    counts_simulated = pd.DataFrame.from_records(throughputs, columns=['type', 'count'])
    counts_simulated['throughput'] = counts_simulated['count'].divide(simulation_days)

    counts_simulated = counts_simulated.groupby('type', as_index=False).sum()

    # counts_simulated = counts_simulated[['type', 'throughput']]

    for type, throughput in throughputs:
        logging.info("Type {}: {} ({} relative share)".format(type, throughput, throughput / total_throughput))

    # Compute Walltimes
    walltime_filenames = [i for i in os.listdir(directory) if
                          os.path.isfile(os.path.join(directory, i)) and external_call_keyword in i]

    walltimes = job_type_response_times(walltime_filenames, directory)

    # Compute passive resource utilization
    logging.info("")
    logging.info("## Jobslot Utilization")

    logging.info("Stop time: {}".format(stoptime))

    passive_file_paths = [path for path in all_files if passive_resource_keyword in path]

    logging.info("Number of passive resource paths: {}".format(len(passive_file_paths)))

    avg_used_jobslots, total_slots = total_passive_resource_utilization(passive_file_paths, stoptime)

    logging.info("Average allocated jobslots: {} (of {} total jobslots)".format(avg_used_jobslots, total_slots))
    logging.info("Average free slots: {}".format(total_slots - avg_used_jobslots))
    logging.info("Jobslot utilization: {}".format(avg_used_jobslots / total_slots))

    metadata = {}
    metadata['core_count'] = core_count
    metadata['stoptime'] = stoptime
    metadata['simulation_days'] = simulation_days
    metadata['total_count'] = total_throughput
    metadata['node_count'] = len(passive_file_paths)
    metadata['total_slots'] = int(total_slots)
    metadata['avg_used_jobslots'] = avg_used_jobslots

    import json
    with open(os.path.join(directory, output_dir, 'metadata.json'), 'w') as outfile:
        json.dump(metadata, outfile)

    walltimes.to_csv(os.path.join(directory, output_dir, 'walltimes.csv'))
    utilization_timeseries.to_csv(os.path.join(directory, output_dir, 'utilization.csv'))
    counts_simulated.to_csv(os.path.join(directory, output_dir, 'throughput-simulated.csv'))


if __name__ == '__main__':
    main()
