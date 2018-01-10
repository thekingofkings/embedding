#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Wed Sep  6 15:38:56 2017

@author: hxw186
"""

import matplotlib.pyplot as plt
import matplotlib
matplotlib.rc('pdf', fonttype=42)
import numpy as np



y = np.array([[0.1807, 0.2315, 0.2114, 0.2004, 0.16076, 0.2691, 0.17433, 0.3071],
   [0.0403, 0.0851, 0.0756, 0.04925, 0.08048, 0.1010, 0.09447, 0.1408]])

y2 = np.array([[0.0904, 0.1915, 0.21575, 0.2142, 0.1490, 0.1717, 0.1681, 0.2505],
               [0.04603, 0.0944, 0.1428, 0.14086, 0.09272, 0.0944, 0.1239, 0.1686]])
y = y.T

xidx = np.array([1,2,3.5,4.5,6,7,8.5,9.5])

x = np.array([xidx, xidx+12])
x = x.T

colors = ['#004482', '#0098f1', '#007600', '#18d617', '#ea8d04', '#dfdd00', '#a11e00', '#f71e00']


f = plt.figure(figsize=(10,4))

for i in range(x.shape[0]):
    plt.bar(x[i], y[i], color=colors[i])
    
plt.axis([-1, 23, 0, 0.42])
plt.legend(['freq','freq-cat','PGT','EBM','node2vec','emb','node2vec-cat','emb-cat'], ncol=4, loc=0, fontsize=15)

plt.xticks([5.25, 17.25], ['all(100%)','#<=2(96%)'])
plt.tick_params(axis='both', which='major', labelsize=16)
plt.xlabel("Meeting frequency", fontsize=20)
plt.ylabel("AUC", fontsize=20)
plt.savefig("gowalla-results.pdf", bbox_inches='tight')

#plt.savefig("brightkite-results.pdf", bbox_inches='tight')