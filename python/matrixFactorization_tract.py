#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Matrix factorization on tract.

Created on Mon Dec 26 17:37:17 2016

@author: hj
"""

import pickle
import numpy as np
import nimfa

numTimeSlot = 8

def getSortedId():
    with open("../miscs/POI_tract.pickle") as fin:
        sortedId = pickle.load(fin)
    return sortedId

sortedId = getSortedId()
    
    
def NMFfeatures(h):
    t = np.genfromtxt("../miscs/taxi-h{0}.vec".format(h), delimiter=" ", skip_header=1)
    tid = t[:,0]
    l = len(tid)
    tid = tid.astype(int)
    tid.sort()
    idx = np.searchsorted(sortedId, tid)
    print "@hour {0}, #regions {1}".format(h, len(idx))
    
    f = np.loadtxt("../miscs/taxi-h{0}.matrix".format(h), delimiter=",")
    fp = f[idx,:]
    fp = fp[:, idx]
    assert fp.shape==(l, l)
    
    nmf = nimfa.Nmf(fp, rank=10, max_iter=30, update="divergence", objective="conn", conn_change=50)
    nmf_fit = nmf()
    src = nmf_fit.basis()
    dst = nmf_fit.coef()
    
    return np.concatenate( (src, dst.T), axis=1 ), tid
    

if __name__ == '__main__':
    nmfeatures = []
    regionIDs = []
    for h in range(numTimeSlot):
        f, tid = NMFfeatures(h)
        nmfeatures.append(f)
        regionIDs.append(tid)
    with open("nmf-tract.pickle", "w") as fout:
        pickle.dump(nmfeatures, fout)
        pickle.dump(regionIDs, fout)



