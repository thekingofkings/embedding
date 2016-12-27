"""
Binary classification evaluation at Community Area level.
"""
from sklearn import tree, svm
from sklearn.model_selection import cross_val_score
import pickle
import numpy as np
import nimfa


def LINEfeatures():
    F = [0]*77
    with open("../miscs/taxi_all.txt") as fin:
        fin.readline()  # header
        for line in fin:
            ls = line.strip().split(" ")
            f = [float(e) for e in ls]
            idx = int(f[0]) - 1
            F[idx] = f[1:]
    return np.array(F)
    

def NMFfeatures():
    f = np.loadtxt("../miscs/taxiFlow.csv", delimiter=",")
    nmf = nimfa.Nmf(f, rank=4, max_iter=30, update="divergence", objective="conn", conn_change=50)
    nmf_fit = nmf()
    src = nmf_fit.basis()
    dst = nmf_fit.coef()
    return np.concatenate( (src, dst.T), axis=1 )
    
    
    
def evaluation_with_classification():
    F = LINEfeatures()
    M = NMFfeatures()
    
    clf1 = tree.DecisionTreeClassifier()
    clf2 = svm.SVC()
    
    l1 = pickle.load(open("../miscs/poi-label"))
    l2 = pickle.load(open("../miscs/demo-label"))
    c = pickle.load(open("../miscs/crime-label"))
    lehd = pickle.load(open("../miscs/lehd-label"))
    l = dict(l1, **l2)
    l["Crime"] = c
    l["LEHD"] = lehd
    for k in l:
        print k
        if sum(l[k]) == 77:
            print "Only one class"
        else:
            scores1 = cross_val_score(clf1, F, l[k], cv=10)
            nmfs1 = cross_val_score(clf1, M, l[k], cv=10)
            print "DT", scores1.mean(), scores1.std(), nmfs1.mean(), nmfs1.std()
            scores2 = cross_val_score(clf2, F, l[k], cv=10)
            nmfs2 = cross_val_score(clf2, M, l[k], cv=10)
            print "SVM", scores2.mean(), scores2.std(), nmfs2.mean(), nmfs2.std()
    return l, F, M
    

if __name__ == "__main__":
    l = evaluation_with_classification()