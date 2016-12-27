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
    

def selectSamples(labels, rids, ordKey):
    idx = np.searchsorted(ordKey, rids)
    return labels[idx]
    
    
def retrieveEmbeddingFeatures():
    embedF = []
    embedRid = []
    for h in range(24):
        t = np.genfromtxt("../miscs/taxi-h{0}.vec".format(h), delimiter=" ", skip_header=1)
        embedRid.append(t[:,0])
        t = t[:, 1:]
        assert t.shape[1]==20
        embedF.append(t)
    return embedF, embedRid

    
    
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
            
    
    
def evalute_by_pairwise_similarity():
    
    
    
    
if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        if sys.argv[1] == "binary":
            evalute_by_binary_classification()
    else:
        print "missing parameter"
    