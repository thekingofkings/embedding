#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Embedding evaluation at tract level.


Created on Mon Dec 26 17:27:45 2016

@author: hj
"""

import pickle
import numpy as np
from sklearn import tree
from sklearn.model_selection import cross_val_score
import matplotlib.pyplot as plt
from scipy.spatial.distance import cosine


def generatePOIlabel_helper(ordKey, tract_poi, poi_type):
    poi_cnt = []
    cnt = 0
    for k in ordKey:
        if k in tract_poi and poi_type in tract_poi[k]:
            poi_cnt.append(tract_poi[k][poi_type])
            cnt += 1
        else:
            poi_cnt.append(0)
    median = np.median(poi_cnt)
    print "{0} #non-zeros {1}, median {2}".format(poi_type, cnt, median)
    label = np.array([1 if val >= median else 0 for val in poi_cnt])
    assert len(label) == len(ordKey)
    return label, median >= 1
    
    
def generatePOIBinarylabel(ordKey, tract_poi):
    header = ['Food', 'Residence', 'Travel', 'Arts & Entertainment', 
        'Outdoors & Recreation', 'College & Education', 'Nightlife', 
        'Professional', 'Shops', 'Event']
        
    L = {}
    for h in header:
        l, flag = generatePOIlabel_helper(ordKey, tract_poi, h)
        if flag:
            L[h] = l
    return L
    
    
def generatePairWiseGT(rids, tract_poi):
    header = ['Food', 'Residence', 'Travel', 'Arts & Entertainment', 
        'Outdoors & Recreation', 'College & Education', 'Nightlife', 
        'Professional', 'Shops', 'Event']
    gnd_vec = {}
    for k in rids:
        if k not in tract_poi:
            poi = [0] * 10
        else:
            poi = []
            for p in header:
                if p in tract_poi[k]:
                    poi.append(tract_poi[k][p])
                else:
                    poi.append(0)
        pv = np.array(poi) / float(sum(poi)) if sum(poi) != 0 else np.array(poi)
        gnd_vec[k] = pv
    
    gnd_pair = {}
    for k in rids:
        cosDist = []
        for k2 in rids:
            if k2 == k:
                continue
            c = cosine(gnd_vec[k], gnd_vec[k2])
            cosDist.append((k2, c))
        cosDist.sort(key=lambda x: x[1])
        gnd_pair[k] = [cosDist[i][0] for i in range(3)]
            
    return gnd_pair
        
            
    

def selectSamples(labels, rids, ordKey):
    idx = np.searchsorted(ordKey, rids)
    return labels[idx]
    
    
def retrieveEmbeddingFeatures():
    embedF = []
    embedRid = []
    for h in range(24):
        t = np.genfromtxt("../miscs/taxi-h{0}.vec".format(h), delimiter=" ", skip_header=1)
        embedRid.append(t[:,0].astype(int))
        t = t[:, 1:]
        assert t.shape[1]==20
        embedF.append(t)
    return embedF, embedRid

    
def pairwiseEstimator(features, rids):
    estimates = {}
    for i, k in enumerate(rids):
        pd = []
        for i2, k2 in enumerate(rids):
            if k2 == k:
                continue
            pd.append( (k2, cosine(features[i], features[i2])) )
        pd.sort(key=lambda x: x[1])
        estimates[k] = pd
    return estimates
    
    
    
    
def evalute_by_binary_classification():
    with open("../miscs/POI_tract.pickle") as fin:
        ordKey = pickle.load(fin)
        tract_poi = pickle.load(fin)
    
    with open("nmf-tract.pickle") as fin2:
        nmfeatures = pickle.load(fin2)
        nmRid = pickle.load(fin2)
        
    embedFeatures, embedRid = retrieveEmbeddingFeatures()
    
    labels = generatePOIBinarylabel(ordKey, tract_poi)
    clf = tree.DecisionTreeClassifier()
    
    plt.figure()
    for i, l in enumerate(labels):
        print "{0}\tMF\tEmbedding".format(l)
        nm = []
        embed = []
        ax = plt.subplot(3,3,i+1)
        for h in range(24):
            assert len(np.intersect1d(nmRid[h], embedRid[h])) == len(embedRid[h])
            L = selectSamples(labels[l], nmRid[h], ordKey)
            score1 = cross_val_score(clf, nmfeatures[h], L, cv=10)
            score2 = cross_val_score(clf, embedFeatures[h], L, cv=10)
            nm.append(score1.mean())
            embed.append(score2.mean())
        ax.plot(nm)
        ax.plot(embed)
        ax.set_title(l)
        ax.legend(['NM', 'Embedding'], loc='best')
    plt.show()
            
    

def topK_accuracy(k, estimator, pair_gnd):
    cnt = 0
    total = len(estimator)
    for rid in estimator:
        knn = [estimator[rid][i][0] for i in range(k)]
        if len(np.intersect1d(pair_gnd[rid], knn)) != 0:
            cnt += 1
    return float(cnt) / total
    
    
    
def evalute_by_pairwise_similarity():
    with open("../miscs/POI_tract.pickle") as fin:
        ordKey = pickle.load(fin)
        tract_poi = pickle.load(fin)
    
    with open("nmf-tract.pickle") as fin2:
        nmfeatures = pickle.load(fin2)
        nmRid = pickle.load(fin2)
        
    embedFeatures, embedRid = retrieveEmbeddingFeatures()
    
    ACC1 = []
    ACC2 = []
    plt.figure()
    for h in range(24):
        pe_embed = pairwiseEstimator(embedFeatures[h], embedRid[h])
        pe_mf = pairwiseEstimator(nmfeatures[h], nmRid[h])    
        pair_gnd = generatePairWiseGT(embedRid[h], tract_poi)
        acc1 = topK_accuracy(20, pe_embed, pair_gnd)
        acc2 = topK_accuracy(20, pe_mf, pair_gnd)
        ACC1.append(acc1)
        ACC2.append(acc2)
        print h, acc1, acc2
    plt.plot(ACC1)
    plt.plot(ACC2)
    plt.legend(["Embedding", "MF"], loc='best')
        
    
    
    
    
    
if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        if sys.argv[1] == "binary":
            evalute_by_binary_classification()
        elif sys.argv[1] == "pairewise":
            evalute_by_pairwise_similarity()
    else:
        print "missing parameter"
    