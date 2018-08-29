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

    directory = sys.argv[1]

    count_files = ['throughput-simulated-physical.csv', 'throughput-simulated-logical.csv',
                   'job_counts_extracted_reports.csv', 'job_counts_reference_jm.csv']
    labels = ['simulated\n(physical cores)', 'simulated\n(logical cores)', 'measured\n(job reports)',
              'measured\n(CMS Dashboard)']

    # count_files = ['throughput-simulated-physical.csv', 'throughput-simulated-logical.csv',
    #                'job_counts_reference_jm.csv']
    # labels = ['simulated\n(physical cores)', 'simulated\n(logical cores)',
    #           'measured\n(CMS Dashboard)']

    counts_list = []
    for label, count_file in zip(labels, count_files):
        counts = pd.read_csv(os.path.join(directory, count_file))

        counts_total = counts['throughput'].sum()

        # counts['source'] = "{}\n({:.0f} total)".format(label, counts_total)
        counts['source'] = label
        counts_list.append(counts)

    all_counts = pd.concat(counts_list)
    pivoted = all_counts.pivot(index='source', columns='type', values='throughput').fillna(0)

    fig, axes = draw_throughput(pivoted)

    axes.set_title('Job throughputs, demands from I/O ratios (May 2018)')
    axes.set_ylabel('')
    axes.set_xlabel('Job throughput / day$^{-1}$')
    axes.legend(title="Job Types")

    fig.tight_layout()


    fig.savefig(os.path.join(directory, 'throughputs.pdf'))


if __name__ == '__main__':
    main()
