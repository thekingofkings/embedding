#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Graph random walks running time evaluation.

Created on Fri May 19 17:16:25 2017

@author: hj
"""

import matplotlib.pyplot as plt


n_seqs = [0.5, 1, 2, 5, 10]
running_time = [[1.038, 2.001, 3.959, 9.739, 19.462],
                [1.291, 2.423, 4.635, 11.496, 23.44],
                [1.815, 3.617, 7.161, 18.003, 35.766],
                [3.467, 6.685, 13.76, 33.652, 65.752]]
lines_setting = ["CA alias table", "CA random interval", "Tract alias table", "Tract random interval"]
lines_style = ["ro-", "bv-", "ro--", "bv--"]


plt.rc("axes", linewidth=2)
f = plt.figure(figsize=(8,6))
for i in range(4):
    plt.plot(n_seqs, running_time[i], lines_style[i], label=lines_setting[i], lw=3, ms=7)
plt.legend(fontsize=20, loc='upper left')
plt.xlabel("Number of random walks to sample (million)", fontsize=20)
plt.ylabel("Running time (second)", fontsize=20)
plt.grid(b=True, axis="both", lw=1)
plt.axis([0, 10.5, 0, 70])
plt.tick_params(labelsize=18)
plt.savefig("running-time.pdf")