#!/usr/bin/env python2
# -*- coding: utf-8 -*-
"""
Created on Fri Apr 28 16:55:14 2017

@author: hj
"""

import pandas as pd
import tensorflow as tf
import numpy as np


def retrieve_data(year):
    df_data = pd.DataFrame.from_csv("ca-features-{0}.dataframe".format(year))
    
    return df_data


def input_fn(df):
    feature_cols = {k: tf.constant(df[k].values, shape=[df[k].size,1]) for k in df.columns[1:]}
    label = tf.constant(df['crime_rate'].values)
    return feature_cols, label


if __name__ == '__main__':
    
    df = retrieve_data(2012)
    df = df.rename(columns=lambda x: x.replace(" ", "_"))
    
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
    
    taxi = tf.contrib.layers.real_valued_column("taxi")
    geo = tf.contrib.layers.real_valued_column("geo")
    
    
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
            poi_night, poi_prof, poi_shops, poi_evnt, taxi, geo],
            optimizer=tf.train.FtrlOptimizer(
            learning_rate=0.1,
            l1_regularization_strength=0,
            l2_regularization_strength=0.001)
        )
        m.fit(input_fn=train_input_fn, steps=10)
        est_bar = m.predict(input_fn=eval_input_fn, outputs=["scores"])
        mae.append(np.abs(df_test["crime_rate"].values - est_bar.next()['scores']))
        
    print np.mean(mae), np.mean(mae)/df["crime_rate"].mean()