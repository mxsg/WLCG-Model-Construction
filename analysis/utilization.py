#!/usr/bin/env python3

# In case this code is run with Python 2
from __future__ import division

import datetime
import logging
import os
import re
import sys

import matplotlib.pyplot as plt
import numpy as np
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


def count_lines(path, headerlen=0):
    return sum(1 for _ in open(path)) - headerlen


def job_type_throughputs(filenames, directory):
    type_regex = r'jobsSystem.provided_role_system_(.*)\.(.*)\.csv'
    result = []

    pattern = re.compile(type_regex)
    job_types = [(pattern.findall(filename)[0][0], filename) for filename in filenames]

    for type, path in job_types:
        job_throughput = count_lines(os.path.join(directory, path), headerlen=1)
        result.append((type, job_throughput))

    return result


def visualize_utilization(utilization_timeseries: pd.DataFrame, path, core_col='busy_cores', reference_utilization=None,
                          max_val=None, resample_freq=600):
    utilization_timeseries['utilization'] = utilization_timeseries[core_col].divide(max_val)

    utilization_timeseries['time_resampled'] = utilization_timeseries.index.to_series().divide(resample_freq).apply(
        np.ceil).multiply(resample_freq)

    utilization_resampled = utilization_timeseries.groupby('time_resampled')['utilization'].mean()

    fig, axes = plt.subplots()

    axes = utilization_resampled.plot.line(ax=axes, label="Simulated (busy share of {} cores)".format(max_val))
    # axes = utilization_resampled.plot.line(ax=axes, label="resampled".format(max_val))

    if max_val is not None:
        axes.set_ylim(0, 1)

    max_time = max(utilization_resampled.index)
    axes.set_xlim(left=0, right=max_time)

    if reference_utilization is not None:
        axes.axhline(reference_utilization, label="Reference (average)", color='#ff7f0e')

    axes.legend()

    axes.set_ylabel("CPU Utilization")
    axes.set_xlabel("Time / s")
    axes.set_title("Simulated and measured CPU utilization")

    fig = axes.get_figure()
    return fig, axes

def read_throughput_description(path):
    df = pd.read_csv(path)
    return df

def draw_throughput(job_type_throughputs):
    fig, axes = plt.subplots()

    # job_type_throughputs.sort_index(axis=1, inplace=True)
    job_type_throughputs.sort_values(by='measured', axis=1, ascending=False, inplace=True)

    job_type_throughputs.plot.barh(ax=axes, stacked=True)

    axes.set_title('Simulated and measured job throughputs (May 2018)')
    axes.set_ylabel('')
    axes.set_xlabel('Job throughput / (1 / day)')
    axes.legend(title="Job Types")

    fig.set_size_inches(8, 4)

    fig.tight_layout()

    return fig, axes


def main():
    if len(sys.argv) != 2:
        print("Usage: python utilization.py <directory>")
        print("<directory> directory look for CSV utilization files in")
        sys.exit(1)

    directory = sys.argv[1]
    output_file = 'results.txt'

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

    # Todo Load this from a file!
    utilization_reference = 0.75
    fig, axes = visualize_utilization(utilization_timeseries, 'utilization.pdf',
                                      reference_utilization=utilization_reference, max_val=core_count)

    fig.savefig(os.path.join(directory, 'utilization.pdf'))

    total_utilization = average_core_utilization(utilization_file_paths)
    logging.info("Total utilization: {}".format(total_utilization))

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

    for type, throughput in throughputs:
        logging.info("Type {}: {} ({} relative share)".format(type, throughput, throughput / total_throughput))

    # Generate figure for job throughputs

    # Todo Compute this directly from the data!
    simulation_days = 7

    counts_measured = read_throughput_description(os.path.join(directory, 'job_counts_reference_jm.csv'))
    counts_measured.rename(columns={'throughput_day': 'throughput'}, inplace=True)

    counts_measured = counts_measured[['type', 'throughput']]
    counts_measured['source'] = 'measured'

    counts_simulated = pd.DataFrame.from_records(throughputs, columns=['type', 'count'])
    counts_simulated['throughput'] = counts_simulated['count'].divide(simulation_days)
    counts_simulated = counts_simulated[['type', 'throughput']]
    counts_simulated['source'] = 'simulated'

    counts = pd.concat([counts_measured, counts_simulated])

    pivoted = counts.pivot(index='source', columns='type', values='throughput').fillna(0)

    fig, axes = draw_throughput(pivoted)

    fig.savefig(os.path.join(directory, 'throughputs.pdf'))


    # counts = pd.merge(counts_measured, counts_simulated, on='type')
    # , 'count_measured', 'count_simulated'
    # counts = counts[['type', 'measured', 'simulated']]

    # counts.sort_values(by='measured', ascending=False, inplace=True)


    # Compute passive resource utilization
    logging.info("")
    logging.info("## Jobslot Utilization")

    # Compute time max length
    stoptime = simulation_stop_time(utilization_file_paths[0])

    logging.info("Stop time: {}".format(stoptime))

    passive_file_paths = [path for path in all_files if passive_resource_keyword in path]

    logging.info("Number of passive resource paths: {}".format(len(passive_file_paths)))

    avg_used_jobslots, total_slots = total_passive_resource_utilization(passive_file_paths, stoptime)

    logging.info("Average allocated jobslots: {} (of {} total jobslots)".format(avg_used_jobslots, total_slots))
    logging.info("Average free slots: {}".format(total_slots - avg_used_jobslots))
    logging.info("Jobslot utilization: {}".format(avg_used_jobslots / total_slots))


if __name__ == '__main__':
    main()
