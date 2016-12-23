#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Case study to support the flow idea.

Ideal case:
    The hypothesis is true: in the morning, people go to office tract from residential area.
    In the afternoon, when office hour is over, they went to nightlife tracts for fun. In the night, they
    went home.

Created on Thu Dec 22 19:02:35 2016

@author: hj
"""

import numpy as np
import matplotlib.pyplot as plt


f = np.loadtxt("../miscs/taxi-flow-time-series.txt", delimiter=",")
n = f.shape[0]
tractId = f[0:n:2, 0]
inflow = f[0:n:2, 1:]
outflow = f[1:n:2, 1:]

inflowSum = inflow.sum(axis=1)
outflowSum = outflow.sum(axis=1)

assert len(inflowSum) == 801

ifSumMedian = np.median(inflowSum)
ofSumMedian = np.median(outflowSum)
ifSumMean = inflowSum.mean()
ofSumMean = outflowSum.mean()

ifRow = np.argwhere((inflowSum > ifSumMedian * 1) & (inflowSum < ifSumMean * 2))
ofRow = np.argwhere((outflowSum > ofSumMedian* 1) & (outflowSum < ofSumMean * 2))
commonRow = np.intersect1d(ifRow, ofRow)

for r in commonRow:
    assert r in ifRow and r in ofRow
    
tractId = tractId[commonRow]
inflow = inflow[commonRow, :]
outflow = outflow[commonRow, :]
n = len(tractId)



from sklearn.cluster import KMeans
NUM_CLUSTER = 4

kms = KMeans(n_clusters=NUM_CLUSTER)
inflowkms = kms.fit(inflow)
outflowkms = kms.fit(outflow)

iflables = inflowkms.labels_
oflables = outflowkms.labels_


c1 = None
c2 = None

plt.figure()
f1 = plt.subplot(1,2,1)
ll = []
for l in range(NUM_CLUSTER):
    samplesRow = np.argwhere(iflables == l)
    if len(samplesRow) == 9:
        c1 = samplesRow
    meanSample = np.mean(inflow[samplesRow,:], axis=0)
    meanSample = meanSample.reshape((24,))
    f1.plot(meanSample)
    legendLabel = "cluster {0} with {1} samples".format(l, len(samplesRow))
    ll.append(legendLabel)
f1.legend(ll)
f1.set_title("Inflow cluster")

f2 = plt.subplot(1,2,2)
for l in range(NUM_CLUSTER):
    samplesRow = np.argwhere(oflables == l)
    if len(samplesRow) == 21:
        c2 = samplesRow
    meanSample = np.mean(outflow[samplesRow,:], axis=0)
    meanSample = meanSample.reshape((24,))
    f2.plot(meanSample)
    legendLabel = "cluster {0} with {1} samples".format(l, len(samplesRow))
    ll.append(legendLabel)
f2.legend(ll)
f2.set_title("Outflow cluster")
ll = []
plt.show()



