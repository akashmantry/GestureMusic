# -*- coding: utf-8 -*-
"""
Created on Wed Apr 12 11:31:27 2017

@author: Akash
"""

import pickle
import numpy as np
from sklearn.metrics import accuracy_score
from sklearn.metrics import confusion_matrix
from map_to_range import map_to_range

with open('E:\Wearable&MobileSensorComputing\data\classifier.pickle', 'rb') as f:
    classifier = pickle.load(f)
    
if classifier == None:
    raise Exception("Classifier is null; make sure you have trained it!")
    

feature_names = ["Max_a_x", "Mean_a_x", "Var_a_x", "Min_a_x", "Median_a_x", "PTP_a_x", "Max_a_y", "Mean_a_y", "Var_a_y", "Min_a_y", "Median_a_y", "PTP_a_y", "Max_a_z", "Mean_a_z", "Var_a_z", "Min_a_z", "Median_a_z", "PTP_a_z", "Max_av_x", "Mean_av_x", "Var_av_x", "Min_av_x", "Median_av_x", "Max_av_y", "Mean_av_y", "Var_av_y", "Min_av_y", "Median_av_y", "Max_av_z", "Mean_av_z", "Var_av_z", "Min_av_z", "Median_av_z", "Distance"]
gesture_mapping = {0 : "volume_up", 1 : "volume_down", 2 : "play_pause", 3 : "next_song", 4 : "previous_song", 5 : "random"}
n_features = len(feature_names)

X = np.zeros((0,n_features))
y = np.zeros(0,)


#==============================================================================
# #for testing
# a = np.loadtxt("E:\Wearable&MobileSensorComputing\data\data_test_features.csv", unpack = True, delimiter= ',')
#  
#  #original=46 columns in sheet
# X = a[0:46]
# y = a[46]
#  
# X = np.transpose(X)
#  
#  #print X
# counter = 0
# #for i in X:
#  #print counter
# #     print "Classifier predicted: " + str(classifier.predict(i.reshape(1,-1)))
#  #print "Actual answer: " + str(y[counter])
# #     counter += 1
# 
# predictions = classifier.predict(X)    
# 
# conf = confusion_matrix(predictions, y, labels=[0,1])
# precision = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=1).astype(float))
# recall = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=0).astype(float))
# accuracy = np.sum(np.diag(conf)) / float(np.sum(conf))
# print accuracy
# print precision
# print recall
# 
# print predictions
# print accuracy_score(y, predictions)
#==============================================================================


def predict(computed_features):
    global X
    global classifier
    X = np.transpose(np.asarray(computed_features).reshape(-1,1))
    prediction = classifier.predict(X)             
    return [computed_features[-1], gesture_mapping[prediction[0]]] #PX:-4, Combined:-1
