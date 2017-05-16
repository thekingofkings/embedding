#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Tensorflow

Hybrid regression - wide and deep oombined regression model.

The wide features are common regression features.
The deep features are sparse features going through a DNN for embedding.


Created on Tue May 16 15:31:45 2017

@author: hj
"""

import pandas as pd
import pickle
import numpy as np
from tf_linear_regression import retrieve_data, input_fn
import tensorflow as tf


def find_top6(m, name):
    r = []
    for row in m:
        idx = np.argsort(row)
        idx = np.flip(idx, axis=0)[0:6]
        r.append(idx)
    return pd.DataFrame(data=r, columns=["{0}_{1}".format(name, i) for i in range(1,7)])


if __name__ == '__main__':
    
    with open("gd-tf.pickle") as fin:
        gd = pickle.load(fin)
        taxi = pickle.load(fin)
       
    gd6 = find_top6(gd, "geo")
    tf6 = find_top6(taxi, "taxi")
    
    df = retrieve_data(2012)
    df = df.rename(columns=lambda x: x.replace(" ", "_"))
    
    df = pd.concat((df, gd6, tf6), axis=1)
    
    for i in range(1, 7):
        tk = "taxi_{0}".format(i)
        df[tk] = df[tk].apply(lambda x: df.ix[x, 'crime_rate'])
        gk = "geo_{0}".format(i)
        df[gk] = df[gk].apply(lambda x: df.ix[x, 'crime_rate'])
        
    
    population = tf.contrib.layers.real_valued_column("total_population")
    pop_density = tf.contrib.layers.real_valued_column("population_density")
    poverty = tf.contrib.layers.real_valued_column("poverty_index")
    disadvantage = tf.contrib.layers.real_valued_column("disadvantage_index")
    resid_stability = tf.contrib.layers.real_valued_column("residential_stability")
    ethnic_diversity = tf.contrib.layers.real_valued_column("ethnic_diversity")
    pct_blk = tf.contrib.layers.real_valued_column("pct_black")
    pct_hisp = tf.contrib.layers.real_valued_column("pct_hispanic")
    
    poi_food = tf.contrib.layers.real_valued_column("Food")
    poi_resd = tf.contrib.layers.real_valued_column("Residence")
    poi_trvl = tf.contrib.layers.real_valued_column("Travel")
    poi_entertain = tf.contrib.layers.real_valued_column("Arts_Entertainment")
    poi_outdoor = tf.contrib.layers.real_valued_column("Outdoors_Recreation")
    poi_edu = tf.contrib.layers.real_valued_column("College_Education")
    poi_night = tf.contrib.layers.real_valued_column("Nightlife")
    poi_prof = tf.contrib.layers.real_valued_column("Professional")
    poi_shops = tf.contrib.layers.real_valued_column("Shops")
    poi_evnt = tf.contrib.layers.real_valued_column("Event")
    
    taxi = []
    geo = []
    for i in range(1,7):
        taxi.append(tf.contrib.layers.real_valued_column("taxi_{0}".format(i)))
        geo.append(tf.contrib.layers.real_valued_column("geo_{0}".format(i)))
    
    
    def train_input_fn():
        return input_fn(df_train)
    
    def eval_input_fn():
        return input_fn(df_test)
    
    from sklearn.model_selection import LeaveOneOut
    
    loo = LeaveOneOut()
    
    mae = []
    for train_idx, test_idx in loo.split(df):
        df_train = df.iloc[train_idx,:]
        df_test = df.iloc[test_idx, :]
        
        m = tf.contrib.learn.LinearRegressor(feature_columns=[
            population, pop_density, poverty, disadvantage, resid_stability, ethnic_diversity, 
            pct_blk, pct_hisp, poi_food, poi_resd, poi_trvl, poi_entertain, poi_outdoor, poi_edu,
            poi_night, poi_prof, poi_shops, poi_evnt] + taxi + geo,
            optimizer=tf.train.FtrlOptimizer(
            learning_rate=0.1,
            l1_regularization_strength=0,
            l2_regularization_strength=0.001)
        )
        m.fit(input_fn=train_input_fn, steps=10)
        est_bar = m.predict(input_fn=eval_input_fn, outputs=["scores"])
        mae.append(np.abs(df_test["crime_rate"].values - est_bar.next()['scores']))
        
    print np.mean(mae), np.mean(mae)/df["crime_rate"].mean()