#!/usr/bin/env python3

# In case this code is run with Python 2
from __future__ import division

import os
import re
import sys

import math
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


def job_type_response_times(filenames, directory):
    type_regex = r'ExternalCall_externalCallType_(.*?)_(.*)\.csv'

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

    axes.set_ylabel("CPU utilization")
    axes.set_xlabel("Time / s")
    axes.set_title("Simulated and measured CPU utilization")

    fig = axes.get_figure()
    return fig, axes


def visualize_utilizations(utilizations: (str, pd.DataFrame), reference_utilizations=None, resample_freq=600,
                           utilization_col='utilization', max_time=604800):
    fig, axes = plt.subplots()
    max_time = None

    for label, ts, color in utilizations:
        ts = ts.copy()

        ts['time_resampled'] = ts['time_start'].divide(resample_freq).apply(np.floor).multiply(resample_freq)
        utilization_resampled = ts.groupby('time_resampled')[utilization_col].mean()

        axes = utilization_resampled.plot.line(ax=axes, label=label, color=color)

        print("Max time: {}".format(max(utilization_resampled.index)))

        if max_time is None:
            max_time = max(utilization_resampled.index)
        elif max_time > max(utilization_resampled.index):
            max_time = max(utilization_resampled.index)

    for label, reference, color in reference_utilizations:
        axes.axhline(reference, label=label, color=color)

    # color='#ff7f0e'

    axes.set_ylim(0, 1)
    max_time = max(utilization_resampled.index)

    axes.set_xlim(left=0, right=max_time)

    axes.legend()

    fig.set_size_inches(8, 3.5)


    axes.set_ylabel("CPU utilization")
    axes.set_xlabel("Time / s")
    axes.set_title("Simulated and measured CPU utilization")

    # fig = axes.get_figure()
    return fig, axes


def visualize_utilizations_resample(utilizations: (str, pd.DataFrame), reference_utilizations=None, resample_freq=600,
                                    utilization_col='utilization', max_time=604800):
    fig, axes = plt.subplots()
    max_time = None

    for label, ts in utilizations:
        ts = ts.copy()

        ts['time_resampled'] = ts.index.to_series().round(-3)
        utilization_resampled = ts.groupby('time_resampled')[utilization_col].mean()

        # ts['time_resampled'] = ts.index.to_series().divide(resample_freq).apply(np.ceil).multiply(resample_freq)
        # utilization_resampled = ts.groupby('time_resampled')[utilization_col].mean()

        axes = utilization_resampled.plot.line(ax=axes, label=label)

        print("Max time: {}".format(max(utilization_resampled.index)))

        if max_time is None:
            max_time = max(utilization_resampled.index)
        elif max_time > max(utilization_resampled.index):
            max_time = max(utilization_resampled.index)

    for label, reference in reference_utilizations:
        axes.axhline(reference, label=label, color='C2')

    # color='#ff7f0e'

    axes.set_ylim(0, 1)
    max_time = max(utilization_resampled.index)

    axes.set_xlim(left=0, right=max_time)

    axes.legend()

    axes.set_ylabel("CPU Utilization")
    axes.set_xlabel("Time / s")
    axes.set_title("Simulated and measured CPU utilization")

    # fig = axes.get_figure()
    return fig, axes


def visualize_walltimes(df: pd.DataFrame, output_path, output_name, bin_count=50, reference: pd.DataFrame = None):
    required_columns = ['type', 'response_time']

    if not all(col in df.columns for col in required_columns):
        raise ValueError("Missing column for creating walltime visualization!")

    jobtypes = df['type'].unique()

    # Setup subplots
    nplots = len(jobtypes)
    ncols = 2
    nrows = math.ceil(nplots / ncols)

    fig, subplot_axes = plt.subplots(ncols=ncols, nrows=nrows)

    for type, i_subplot in zip(jobtypes, range(len(jobtypes))):
        axes = subplot_axes[i_subplot // ncols, i_subplot % ncols]

        jobs_of_type = df[df['type'] == type]
        reference_of_type = reference[reference['type'] == type]

        n, bins, patches = axes.hist(jobs_of_type['response_time'], density=True, bins=bin_count, label='simulated',
                                     histtype='stepfilled')
        axes.hist(reference_of_type['walltime'], bins=bins, density=True, label='measured', histtype='step')

        simulated_average = jobs_of_type['response_time'].mean()
        measured_average = reference_of_type['walltime'].mean()

        axes.legend(['Simulated (average {:.0f}s)'.format(simulated_average),
                     'Measured (average {:.0f}s)'.format(measured_average)])

        axes.set_xlabel('Walltime / s')
        axes.set_ylabel('Probability Density')

        axes.set_title("{} ({} simulated jobs)".format(type, jobs_of_type.shape[0]))

        # fig.tight_layout()

        # jobs_of_type['response_time'].plot.hist(ax=axes, bins=bins)

        # fig_path = os.path.join(output_path, "{}_{}.pdf".format(output_name, type))
        # fig.savefig(fig_path)

    fig.set_size_inches(14, 12)
    fig.tight_layout()

    # Remove last plot if odd number of plots is encountered
    if nplots % ncols != 0:
        fig.delaxes(subplot_axes[nplots // ncols, nplots % 2])

    # Add overview figure to the report
    fig.savefig('walltime_distributions.pdf')


def read_throughput_description(path):
    df = pd.read_csv(path)
    return df


def draw_throughput(job_type_throughputs):
    fig, axes = plt.subplots()

    # job_type_throughputs.sort_index(axis=1, inplace=True)
    job_type_throughputs.sort_values(by='simulated\n(logical cores)', axis=1, ascending=False, inplace=True)

    job_type_throughputs.plot.barh(ax=axes, stacked=True)

    axes.set_title('Job throughputs, independent CPU and I/O distributions (May 2018)')
    axes.set_ylabel('')
    axes.set_xlabel('Job throughput / day$^{-1}$')
    axes.legend(title="Job Types")

    fig.set_size_inches(8, 4.5)

    fig.tight_layout()

    return fig, axes


def draw_job_type_composition(job_type_throughputs):
    fig, axes = plt.subplots()

    # job_type_throughputs.sort_index(axis=1, inplace=True)
    job_type_throughputs.sort_values(by=job_type_throughputs.first_valid_index(), axis=1, ascending=False, inplace=True)
    job_type_throughputs = job_type_throughputs.div(job_type_throughputs.sum(axis=1), axis=0)

    job_type_throughputs.plot.barh(ax=axes, stacked=True)

    axes.set_title('Job throughputs, independent CPU and I/O distributions (May 2018)')
    axes.set_ylabel('')
    axes.set_xlabel('Job throughput / day$^{-1}$')
    axes.legend(title="Job Types")

    fig.set_size_inches(8, 4)

    fig.tight_layout()

    return fig, axes


def main():
    if len(sys.argv) != 2:
        print("Usage: python utilization.py <directory>")
        print("<directory> directory look for CSV utilization files in")
        sys.exit(1)

    config_path = sys.argv[1]
    base_path = os.path.abspath(os.path.join(config_path, os.pardir))

    results_dir = 'results'
    utilization_filename = 'utilization.csv'

    import json

    with open(config_path, 'r') as file:
        config = json.load(file)

    simulated_utilizations = {}
    for simulation_run in config['simulations']:
        utilization = pd.read_csv(os.path.join(base_path, simulation_run['path'], results_dir, utilization_filename))
        simulated_utilizations[simulation_run['key']] = (simulation_run, utilization)

    utilizations = []

    for key, (simulation_run, utilization) in simulated_utilizations.items():
        average = utilization['utilization'].mean()
        label = "{}, {} (average {:.2f}%)".format(simulation_run['label'], simulation_run['labelDescription'], average * 100)

        utilizations.append((label, utilization, simulation_run['color']))

    references = []

    for reference in config['references']:
        references.append(
            ("{}, {} (average {}%)".format(reference['label'], reference['labelDescription'], reference['value'] * 100), reference['value'], reference['color']))

    fig, axes = visualize_utilizations(utilizations, references, utilization_col='utilization')

    axes.set_title("CPU utilization, {} (May 2018)".format(config['title']))

    fig.tight_layout()

    os.makedirs(os.path.join(base_path, config['output']), exist_ok=True)
    fig.savefig(os.path.join(base_path, config['output'], 'utilizations.pdf'))


if __name__ == '__main__':
    main()
