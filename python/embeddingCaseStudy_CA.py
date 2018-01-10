#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Embedding case study at CA level.


Created on Sun Jan 22 16:08:27 2017

@author: hj
"""

import pickle
import numpy as np
import matplotlib.pyplot as plt
from embeddingEvaluation_tract import retrieveCrossIntervalEmbeddings, getLEHDfeatures_helper, mergeBlockCensus
from sklearn.cluster import KMeans
import matplotlib
matplotlib.rc('pdf', fonttype=42)


def tract_to_CA():
    import shapefile
    sf = shapefile.Reader("../data/Census-Tracts-2010/chicago-tract")
    
    tract2CA = {}
    for rec in sf.records():
        tid = int(rec[2])
        CAid = int(rec[6])
        tract2CA[tid] = CAid
    
    return tract2CA


def CA_poi():
    t2c = tract_to_CA()
    with open("../miscs/POI_tract.pickle") as fin:
        ordKey = pickle.load(fin)
        tract_poi = pickle.load(fin)
    ca_poi = {}
    for tid in t2c:
        if tid not in tract_poi:
            print tid
            continue
        cid = t2c[tid]
        if cid not in ca_poi:
            ca_poi[cid] = tract_poi[tid]
        else:
            for k, v in tract_poi[tid].items():
                if k in ca_poi[cid]:
                    ca_poi[cid][k] = ca_poi[cid][k] + v
                else:
                    ca_poi[cid][k] = v
    return ca_poi
    

def get_CAfeatures_from_tractFeatures(tacs, t2c):
    cacs = {}
    for tid in tacs:
        cid = t2c[tid]
        if cid not in cacs:
            cacs[cid] = tacs[tid]
        else:
            cacs[cid] = mergeBlockCensus(cacs[cid], tacs[tid])
            
    ids = []
    x = []
    for tid, vec in cacs.items():
        vec_sum = float(sum(vec))
        if vec_sum != 0:
            ids.append(tid)
            v = vec / vec_sum
            x.append(v)
            
    return np.array(ids), x



def generatePOIClusteringlabel(nclusters=3):
    t2c = tract_to_CA()
    with open("../miscs/POI_tract.pickle") as fin:
        ordKey = pickle.load(fin)
        tract_poi = pickle.load(fin)
        
    n = len(ordKey)
    header = ['Food', 'Residence', 'Travel', 'Arts & Entertainment', 
        'Outdoors & Recreation', 'College & Education', 'Nightlife', 
        'Professional', 'Shops', 'Event']
    tpois = {}
    for i, k in enumerate(ordKey):
        row = np.zeros((len(header),))
        if k in tract_poi:
            for j, h in enumerate(header):
                if h in tract_poi[k]:
                    row[j] = tract_poi[k][h]
        tpois[k] = row
    ids, x = get_CAfeatures_from_tractFeatures(tpois, t2c)
    
    cls = KMeans(n_clusters=nclusters)
    res = cls.fit(x)
    return ids, res.labels_





def generate_AC_clusteringLabel(ncluster=3, racORwac="rac"):
    t2c = tract_to_CA()
    if racORwac == "rac":
        fn = "../data/il_rac_S000_JT03_2013.csv"
    elif racORwac == "wac":
        fn = "../data/il_wac_S000_JT00_2013.csv"
        
    tacs = getLEHDfeatures_helper(fn, t2c.keys())
    
    ids, x = get_CAfeatures_from_tractFeatures(tacs, t2c)
    
    cls = KMeans(n_clusters=ncluster)
    res = cls.fit(x)
    return ids, res.labels_



def generate_od_clusteringLabel(ncluster=2):
    t2c = tract_to_CA()
    racs = getLEHDfeatures_helper("../data/il_rac_S000_JT03_2013.csv", t2c.keys())
    wacs = getLEHDfeatures_helper("../data/il_wac_S000_JT00_2013.csv", t2c.keys())
    acs = {}
    for tid in t2c:
        v1 = racs[tid] if tid in racs else np.zeros((1,))
        v2 = wacs[tid] if tid in wacs else np.zeros((1,))
        v = np.concatenate((v1, v2))
        acs[tid] = v
    ids, x = get_CAfeatures_from_tractFeatures(acs, t2c)
    labels = np.array([0 if e[0] > e[1] else 1 for e in x])
    
    return ids, labels



def visualizeEmbedding_2D_withCluster(ncluster=3):
    twoGraphEmbeds, twoGRids = retrieveCrossIntervalEmbeddings("../miscs/taxi-deepwalk-CA-usespatial.vec", skipheader=0)
    
    gndTid, gndLabels = generate_AC_clusteringLabel(ncluster, "rac")
#    gndTid, gndLabels = generatePOIClusteringlabel(ncluster)
#    gndTid, gndLabels = generate_od_clusteringLabel(ncluster)
    clrs = ["b", "r", "g", "w", "c", "b"]
    print [ len(np.argwhere(gndLabels==i)) for i in range(ncluster)]
    
    
    plt.figure(figsize=(40,36))
    plt.suptitle("RAC count as ground truth {0} clusters".format(ncluster))
    for h in range(len(twoGRids)):
        plt.subplot(4,6,h+1)
        for cluster in range(ncluster):
            groupIds = gndTid[np.argwhere(gndLabels==cluster)]
            idx = np.in1d(twoGRids[h], groupIds)
            x = twoGraphEmbeds[h][idx,0]
            y = twoGraphEmbeds[h][idx,1]
            ids = twoGRids[h][idx]
            
            plt.scatter(x, y, c=clrs[cluster], hold=True)
            plt.xlim([-1.0, 1])
            plt.ylim([-1, 0.5])
            for i, e in enumerate(ids):
                plt.annotate(s = str(e), xy=(x[i], y[i]), xytext=(-5, 5), textcoords="offset points")
            plt.title("2D visualization at {0}".format(h))
    plt.savefig("CA-RAC-{0}cluster.png".format(ncluster))   
        
    
def visualizeEmbedding_2D():
    twoGraphEmbeds, twoGRids = retrieveCrossIntervalEmbeddings("../miscs/2013/taxi-deepwalk-CA-usespatial-2D.vec", skipheader=0)
    groups = [[13,14,15,16], [8,32,33], [44,45,47,48], [76]]
#    groups = [[5,6,7,21,22], [8,32,33], [26,27,29,30]]
    clrs = ["b", "r", "g", "c", "w", "k"]
    
    plt.figure(figsize=(12,6))
    for k, h in enumerate([7,8,9,16,17,18,21,22]): # enumerate(range(len(twoGRids))): 
        f = plt.subplot(2,4,k+1)
        
        xr = 0
        yr = 0
        xo = 0
        yo = 0
        for i, group in enumerate(groups):
            idx = np.in1d(twoGRids[h], group)
            x = twoGraphEmbeds[h][idx,0]
            y = twoGraphEmbeds[h][idx,1]
            ids = twoGRids[h][idx]
            
            plt.scatter(x, y, s=40, c=clrs[i], hold=True)
#            plt.xlim([-3.0, 0.6])
#            plt.ylim([-2, 0.5])
            for j, e in enumerate(ids):
                if e in [76, 47]:
                    plt.annotate(s = str(e), xy=(x[j], y[j]), xytext=(-2, -15), textcoords="offset points", fontsize=14)
                elif e in [14,15,16]:
                    xr += x[j]
                    yr += y[j]
                elif e in [8, 32, 33]:
                    xo += x[j]
                    yo += y[j]
                else:
                    if k == 0:
                        if e == 13:
                            plt.annotate(s = str(e), xy=(x[j], y[j]), xytext=(-2, -15), textcoords="offset points", fontsize=14)
                        else:
                            plt.annotate(s = str(e), xy=(x[j], y[j]), xytext=(0, -15), textcoords="offset points", fontsize=14)
        
            
        
        if k == 0:
            plt.annotate(s = "14,15,16", xy=(xr/3, yr/3), xytext=(5, 3), textcoords="offset points", fontsize=14)
            plt.annotate(s = "8,32,33", xy=(xo/3, yo/3), xytext=(-50, 8), textcoords="offset points", fontsize=14)
                    
            
        plt.title("{0}:00".format(h), fontsize=16)
        f.tick_params(axis="both", labelsize=10)
    
    plt.tight_layout()
    plt.savefig("CA-case-3region.pdf")
    
    
    
def getTaxiFlow():
    flows = {}
    for h in range(24):
        f = np.loadtxt("../miscs/taxi-CA-h{0}.matrix".format(h), delimiter=" ")
        flows[h] = f
    return flows


def visualizeFlow():
    f = getTaxiFlow()
    cas = [13,14,15,16] #[44,45,47,48] # [26, 27, 29, 30] # [8,32,33]
    plt.figure(figsize=(22,14))
    plt.suptitle("Case study for region {0}".format(cas))
    for h in range(24):
        f = plt.subplot(4,6,h+1)
        lg = []
        for ca in cas:
            plt.plot(f[h][ca-1,:])
            lg.append(str(ca))
        plt.legend(lg)
        plt.title(str(h))
    plt.savefig("CA-case-ts-r3.png")
    
    for ca in cas:
        print ca, sum([sum(f[h][ca-1,:]) for h in range(24)])
    
    
if __name__ == "__main__":
#    visualizeFlow()
    visualizeEmbedding_2D()
#    visualizeEmbedding_2D_withCluster(3)
