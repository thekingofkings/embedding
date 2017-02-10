#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Taxi flow feature genration at tract level.

Goal:
    1. Generate the Matrix factorization features for taxi flow
    2. Generate the LINE embeddings features for taxi flow
    3. Generate the deepwalk embedding features for taxi flow and spatial flow.

Dependency:
    1. The taxi-CA-h{0}.matrix files, on which the MF is applied to learn representations.
    2. The taxi-CA-h{0}.vec files, which are the LINE embeddings learnt from each hour.
    3. The taxi-deepwalk-CA-usespatial.vec, which is the deepwalk embedding learnt from heterogenous graphs.
    
Created on Sun Jan 15 20:25:37 2017

@author: hj
"""

import numpy as np
import nimfa
from embeddingEvaluation_tract import retrieveEmbeddingFeatures_helper, retrieveCrossIntervalEmbeddings
import pickle
import sys

year = 2013

def NMFfeatures_helper(h):    
    f = np.loadtxt("../miscs/{0}/taxi-h{1}.matrix".format(year, h), delimiter=",")
    d1, d2 = f.shape
    assert d1 == d2 and d1 == 801
    
    nmf = nimfa.Nmf(f, rank=10, max_iter=100) #, update="divergence", objective="conn", conn_change=50)
    nmf_fit = nmf()
    src = nmf_fit.basis()
    dst = nmf_fit.coef()
    res = np.concatenate( (src, dst.T), axis=1 )
    assert res.shape == (801, 20)
    return res
  

def getNMFfeatures():
    """
    Return a dictionary, where key is the hour index, and value is the feature representations
    """
    res = {}
    for h in range(8):
        res[h] = NMFfeatures_helper(h)
        assert res[h].shape == (801, 20)
    return res
    

def getLINEembeddingFeatures():
    with open("../miscs/POI_tract.pickle") as fin:
        ordKey = pickle.load(fin)
        tract_poi = pickle.load(fin)
    emptyEmbedding = np.zeros((20,))
        
    res = {}
    for h in range(8):
        f, ids = retrieveEmbeddingFeatures_helper("../miscs/{0}/taxi-h{1}.vec".format(year, h))
        
        sids = np.argsort(ids)
        features = f[sids, :]
        if len(sids) != 801:
            print len(sids), h
            for i, k in enumerate(ordKey):
                if k not in ids:
                    features = np.insert(features, i, emptyEmbedding, axis=0)
        res[h] = features
        assert res[h].shape == (801, 20)
    return res
    
    
def getDeepwalkEmbeddingFeatures(Spatial):
    with open("../miscs/POI_tract.pickle") as fin:
        ordKey = pickle.load(fin)
        tract_poi = pickle.load(fin)
        
    res = {}
    emptyEmbedding = np.zeros((20,))
    features, ids = retrieveCrossIntervalEmbeddings("../miscs/{0}/taxi-deepwalk-tract-{1}.vec".format(year, Spatial), skipheader=0)
    for h in range(8):
        sids = np.argsort(ids[h])
        res[h] = features[h][sids, :]
        
        if len(sids) != 801:
            print len(sids), h
            for i, k in enumerate(ordKey):
                if k not in ids[h]:
                    res[h] = np.insert(res[h], i, emptyEmbedding, axis=0)
        assert res[h].shape == (801, 20)
    return res


if __name__ == "__main__":
    year = 2013
    mf = getNMFfeatures()
    line = getLINEembeddingFeatures()
    dwt = getDeepwalkEmbeddingFeatures("nospatial")
    dws = getDeepwalkEmbeddingFeatures("onlyspatial")
    hdge = getDeepwalkEmbeddingFeatures("usespatial")
    with open("../miscs/{0}/tractflowFeatures.pickle".format(year), "w") as fout:
        pickle.dump(mf, fout)
        pickle.dump(line, fout)
        pickle.dump(dwt, fout)
        pickle.dump(dws, fout)
        pickle.dump(hdge, fout)
        