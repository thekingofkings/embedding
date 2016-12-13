from sklearn import tree, svm
from sklearn.model_selection import cross_val_score
import pickle
import numpy as np


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
    
    
def classification_LINE_features():
    l = pickle.load(open("../miscs/crime-label"))
    F = LINEfeatures()
    
    print "Crime"
    clf1 = tree.DecisionTreeClassifier()
    scores1 = cross_val_score(clf1, F, l, cv=10)
    print "DT", scores1.mean(), scores1.std()
    
    clf2 = svm.SVC()
    scores2 = cross_val_score(clf2, F, l, cv=10)
    print "SVM", scores2.mean(), scores2.std()
    
    l1 = pickle.load(open("../miscs/poi-label"))
    l2 = pickle.load(open("../miscs/demo-label"))
    l = dict(l1, **l2)
    for k in l:
        print k
        if sum(l[k]) == 77:
            print "Only one class"
        else:
            scores1 = cross_val_score(clf1, F, l[k], cv=10)
            print "DT", scores1.mean(), scores1.std()
            scores2 = cross_val_score(clf2, F, l[k], cv=10)
            print "SVM", scores2.mean(), scores2.std()
    return l
    

if __name__ == "__main__":
    l = classification_LINE_features()
    
    f = np.loadtxt("../miscs/taxiFlow.csv", delimiter=",")
    import nimfa
    nmf = nimfa.Nmf(f, rank=10, max_iter=30, update="divergence", objective="conn", conn_change=50)
    nmf_fit = nmf()