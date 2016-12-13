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
    
    clf1 = tree.DecisionTreeClassifier()
    scores1 = cross_val_score(clf1, F, l, cv=10)
    print scores1.mean(), scores1
    
    clf2 = svm.SVC()
    scores2 = cross_val_score(clf2, F, l, cv=10)
    print scores2.mean(), scores2    
    

if __name__ == "__main__":
    f = np.loadtxt("../miscs/taxiFlow.csv", delimiter=",")
    import nimfa
    nmf = nimfa.Nmf(f, rank=10, max_iter=30, update="divergence", objective="conn", conn_change=50)
    nmf_fit = nmf()