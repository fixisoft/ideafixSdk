#!/usr/bin/python
# Copyright (c) Pierre-Yves Peton 2024. All rights reserved

import argparse
import time
from collections import OrderedDict
from datetime import datetime, timezone
from matplotlib.ticker import ScalarFormatter

import matplotlib.pyplot as plt
import numpy as np

START_SENDING_TIME_TAG = "\u000152="
SOH = "\u0001"
EPOCH = datetime.fromtimestamp(0, timezone.utc).replace(tzinfo=None)


def scan(f, t, res, useHeaderTimestamp):
    """perform a first pass to count the total number of messages with a given tag, build a sorted index in the process
    to make faster re-sampling later on"""
    index = OrderedDict()
    count = 0
    timestamp = 0
    next_timestamp = 0
    line_number = 0
    with open(f, 'r') as f:
        line = f.readline()
        line_number += 1
        while line:
            if t in line:
                count += 1
                # Assuming next_timestamp is an integer
                if useHeaderTimestamp:
                    next_timestamp = read_header_timestamp(line)
                else:
                    next_timestamp = read_sender52_timestamp(line)
                if (next_timestamp - timestamp) > res:
                    index[next_timestamp] = line_number, f.tell()
                    timestamp = next_timestamp
            line = f.readline()
            line_number += 1
    index[next_timestamp] = line_number
    return count, index


def read_header_timestamp(line):
    """read the nano timestamps straight from a header. this data is provided by the application and is more precise
    than the counterparty sending time"""
    try:
        return int(line.split(';')[0])
    except ValueError:
        print(f"Warning: '{line.split(';')[0]}' before ';' on line {line} is not an integer.")


def read_sender52_timestamp(line):
    """extract the timestamp under tag 52 and convert it into a nano timestamp (diffed with EPOCH to avoid overflow)"""
    try:
        start_index = line.find(START_SENDING_TIME_TAG)
        end_index = line.find(SOH, start_index + 1)
        delta = datetime.fromisoformat(line[slice(start_index + 4, end_index)]) - EPOCH
        return int(delta.total_seconds() * 1e9)
    except ValueError:
        print(f"Warning: could not find SendingTime/52 timestamp on line {line}")


def tail(ordered_dict, key):
    """it's not straight forward in python to traverse the binary tree behind, there's no tailMap method and iterator
    are not bidirectional"""
    tail_key = None
    for k in ordered_dict:
        if k <= key:
            tail_key = k
        else:
            return tail_key
    return None


def sample(f, t, start_timestamp, index, size, useHeaderTimestamp):
    """count number of occurrences of tag and last timestamp in a given range """
    start_line, position = index[tail(index, start_timestamp)]
    count = 0
    ts = None
    f.seek(position)
    while True:
        line = f.readline()
        if not line:
            break
        if useHeaderTimestamp:
            ts = read_header_timestamp(line)
        else:
            ts = read_sender52_timestamp(line)
        if ts < start_timestamp:
            start_line += 1
            continue
        if t in line:
            count += 1
            if count >= size:
                break
    return ts, start_line, count


def hdr_histogram(data, num_bins=100, hdr_percentile=99.0):
    """Compute logarithmically spaced bins for HDR histogram."""
    hdr_max = np.percentile(data, hdr_percentile)
    hdr_min = np.min(data)  # Exclude non-positive values
    return np.logspace(np.log10(hdr_min), np.log10(hdr_max), num_bins)


if __name__ == "__main__":
    try:
        parser = argparse.ArgumentParser(
            description='compute RTT by resampling message history. provide a HDR histogram and percentiles thanks to '
                        'a bootstrap method.')
        parser.add_argument('filename', help='message history to process')
        parser.add_argument('--resolution', dest='resolution', default=5000000, type=int,
                            help='define the sampling resolution in nanoseconds (default is 5000000ns = 5ms)')
        parser.add_argument('--sampleCount', dest='sampleCount', default=10000, type=int,
                            help='define the number of samples, higher means better estimates (default is 10000)')
        parser.add_argument('--warmup', dest='warmup', default=60, type=int,
                            help='define the warmup period to ignore during sampling (default is 60s)')
        parser.add_argument('--tag', dest='tag', default="35=D", help='defines the tag to look for (default is 35=D)')
        parser.add_argument('--useHeaderTimestamp', action=argparse.BooleanOptionalAction, default=False,
                            help='use header timestamp (a long number before separator \';\', faster and more precise)')
        args = parser.parse_args()

        filename = args.filename
        resolution = args.resolution
        sampleCount = args.sampleCount
        tag = args.tag
        useHeaderTimestamp = args.useHeaderTimestamp
        if useHeaderTimestamp:
            print("using header timestamp - each line must start with a nanosecond timestamp before separator ';'")
        else:
            print("using timestamp in mandatory tag 52/SendingTime (slower and less precise)")
        warmup = 1000000000 * args.warmup
        percentiles = [1,5,50, 90, 95, 99, 99.99]

        print("first scanning ...")
        line_count, index = scan(filename, tag, resolution / 2, useHeaderTimestamp)
        print(f"Number of occurrences of tag '{tag}' = {line_count}")

        first_ts = next(iter(index))
        last_ts = next(reversed(index))
        pop_mean = float(last_ts - first_ts) / float(line_count)
        if last_ts - first_ts < warmup:
            warmup = 0
        first_ts += warmup
        last_ts -= 2 * resolution  # this to avoid hitting the end of the file (in most cases...)
        print(f"population mean = {pop_mean:.2f} ns")
        sample_size = round(float(resolution * line_count) / float(last_ts - first_ts))
        print(f"sample size = {sample_size}")
        means = []
        rng = np.random.default_rng(time.time_ns())
        r_ints = np.sort(rng.integers(low=first_ts, high=last_ts, size=sampleCount))
        i = 0
        len_sample = len(str(sampleCount))
        print(f"start bootstrapping {sampleCount} samples")
        with open(filename, 'r') as file:
            for start in r_ints:
                # real_sample_size allows to manage the case of EOF
                (end, start_line, real_sample_size) = sample(file, tag, start, index, sample_size, useHeaderTimestamp)
                mean = float(end - start) / float(real_sample_size - 1)
                means.append(mean)
                i += 1
                print(f"{str(i).zfill(len_sample)}/{sampleCount} - start {start} - line {start_line:10} -> mean RTT = {mean:10.2f} ns")

        print(f"population mean = {pop_mean:.2f} ns")
        print(f"sampled mean = {np.mean(means):.2f} ns")
        print(f"std dev. = {np.std(means):.2f} ns")
        print(f"sample size = {sample_size}")

        
        # Calculate the specified percentiles
        percentile_values = np.percentile(means, percentiles)
        print("Percentiles:")
        for p, value in zip(percentiles, percentile_values):
            print(f"\t{p:.2f}th percentile = {value:.2f} ns")

        # Compute the HDR histogram bins
        hdr_bins = hdr_histogram(means)
        # Compute the histogram
        hist, bins = np.histogram(means, bins=hdr_bins)
        # Plot the HDR histogram
        plt.style.use('seaborn-whitegrid')
        plt.bar(bins[:-1], hist, width=np.diff(bins), align='edge')
        plt.xlabel('RTT in ns', color='#4f4e4e')
        plt.ylabel(f"{tag} count", color='#4f4e4e')
        plt.title(f"HDR Histogram {str(sampleCount)} samples: resolution = {str(resolution / 1000000)}ms, "
                  f"warmup = {args.warmup}s",fontsize = 10)
        ax = plt.gca()
        
        #removing top and right borders
        ax.spines['top'].set_visible(False)
        ax.spines['right'].set_visible(False)
        
        # tweak the axis labels
        xlab = ax.xaxis.get_label()
        ylab = ax.yaxis.get_label()
        xlab.set_style('italic')
        xlab.set_size(10)
        ylab.set_style('italic')
        ylab.set_size(10)
        
        # tweak the title
        ttl = ax.title
        ttl.set_weight('bold')
      
        #adds major gridlines
        #ax.grid(color='grey', linestyle='-', linewidth=0.25, alpha=0.5)
        
        pdf_file = filename + ".histo.pdf"
        plt.savefig(pdf_file,dpi=1000)
        print(f"HDR histogram saved in {pdf_file}") 
    except FileNotFoundError:
        print("File not found.")
    except Exception as e:
        print("An error occurred:", e)
