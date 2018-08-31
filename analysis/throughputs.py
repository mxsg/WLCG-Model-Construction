#!/usr/bin/env python3

# In case this code is run with Python 2
from __future__ import division

import os
import sys

import matplotlib.pyplot as plt
import pandas as pd


def draw_throughput(job_type_throughputs):
    fig, axes = plt.subplots()

    # job_type_throughputs.sort_index(axis=1, inplace=True)
    job_type_throughputs.sort_values(by=job_type_throughputs.first_valid_index(), axis=1, ascending=False, inplace=True)

    job_type_throughputs.plot.barh(ax=axes, stacked=True)

    fig.set_size_inches(8, 4)

    return fig, axes


def main():
    if len(sys.argv) != 2:
        print("Usage: python utilization.py <directory>")
        print("<directory> directory look for CSV utilization files in")
        sys.exit(1)

    config_path = sys.argv[1]
    results_dir = 'results'
    throughput_filename = 'throughput.csv'
    base_path = os.path.abspath(os.path.join(config_path, os.pardir))

    import json

    with open(config_path, 'r') as file:
        config = json.load(file)

    counts_list = []

    throughputs = {}
    for simulation_run in config['simulations']:
        throughput = pd.read_csv(os.path.join(base_path, simulation_run['path'], results_dir, throughput_filename))
        throughputs[simulation_run['key']] = (simulation_run, throughput)

        counts_total = throughput['throughput'].sum()
        throughput['source'] = "{}\n({})".format(simulation_run['label'], simulation_run["labelDescription"])
        counts_list.append(throughput)

    for reference in config['throughputReferences']:
        throughput = pd.read_csv(os.path.join(base_path, reference['path']))
        throughputs[reference['key']] = (reference, throughput)

        counts_total = throughput['throughput'].sum()
        throughput['source'] = "{}\n({})".format(reference['label'], reference["labelDescription"])
        counts_list.append(throughput)

    all_counts = pd.concat(counts_list)
    pivoted = all_counts.pivot(index='source', columns='type', values='throughput').fillna(0)

    fig, axes = draw_throughput(pivoted)

    axes.set_title('Job throughputs, {} ({})'.format(config["title"], config["datasetName"]))
    axes.set_ylabel('')
    axes.set_xlabel('Job throughput / day$^{-1}$')
    axes.legend(title="Job types")

    fig.set_size_inches(8, 3)
    fig.tight_layout()

    os.makedirs(os.path.join(base_path, config['output']), exist_ok=True)
    fig.savefig(os.path.join(base_path, config['output'], 'throughputs.pdf'))


if __name__ == '__main__':
    main()
