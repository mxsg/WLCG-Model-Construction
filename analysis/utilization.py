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
