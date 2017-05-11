# -*- coding: utf-8 -*-
"""
Created on Sat Apr 08 21:53:58 2017

@author: Akash
"""

import numpy as np
from sklearn.metrics import confusion_matrix
from sklearn.metrics import accuracy_score #to get the accuracy of our classifier
from sklearn import tree  # importing decision tree classifier
from sklearn import svm
from sklearn.cross_validation import train_test_split #split the data into train and test
from sklearn.ensemble import RandomForestClassifier
from sklearn import cross_validation
import sys
import pickle

feature_names = ["Max_a_x", "Mean_a_x", "Var_a_x", "Min_a_x", "Median_a_x", "PTP_a_x", "Max_a_y", "Mean_a_y", "Var_a_y", "Min_a_y", "Median_a_y", "PTP_a_y", "Max_a_z", "Mean_a_z", "Var_a_z", "Min_a_z", "Median_a_z", "PTP_a_z", "Max_av_x", "Mean_av_x", "Var_av_x", "Min_av_x", "Median_av_x", "Max_av_y", "Mean_av_y", "Var_av_y", "Min_av_y", "Median_av_y", "Max_av_z", "Mean_av_z", "Var_av_z", "Min_av_z", "Median_av_z", "Distance"]
n_features = len(feature_names)
X = np.zeros((0,n_features))
y = np.zeros(0,)
a = np.loadtxt("E:\Wearable&MobileSensorComputing\data\data_features.csv", unpack = True, delimiter= ',')

#original=46 columns in sheet
X = a[0:34]
y = a[34]

X = np.transpose(X)
#X = X[:-2]
#new_data_down = X[-1]
#new_data_up = X[-2]
#y = y[:-1] 

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size = .5)

#==============================================================================
# # Decison tree classifier
# 
# my_classifier = tree.DecisionTreeClassifier()
# my_classifier.fit(X_train, y_train) #training data
# predictions = my_classifier.predict(X_test) #testing data
# # show the comparison between the predicted and ground-truth labels
# conf = confusion_matrix(y_test, predictions, labels=[0,1])
# 
# print "Decision tree's Parameters from Sean's code: "        
# accuracy = np.sum(np.diag(conf)) / float(np.sum(conf))
# precision = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=1).astype(float))
# recall = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=0).astype(float))
# print "The accuracy is {}".format(accuracy)
# print "The precision is {}".format(precision)
# print "The recall is {}".format(recall)
# print "Score of decision tree classifier: "
# print accuracy_score(y_test, predictions) # comparing classifier predcitions to the actual labels
#==============================================================================

#==============================================================================
# # SVM classifier
# 
# clf = svm.SVC(gamma=0.001, C=100)
# clf.fit(X_train, y_train)
# predictions = clf.predict(X_test) #testing data
# 
# conf = confusion_matrix(y_test, predictions, labels=[0,1])
# print "SVM's Parameters from Sean's code: "        
# accuracy = np.sum(np.diag(conf)) / float(np.sum(conf))
# precision = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=1).astype(float))
# recall = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=0).astype(float))
# print "The accuracy is {}".format(accuracy)
# print "The precision is {}".format(precision)
# print "The recall is {}".format(recall)
# print "Score of SVM classifier: "
# print accuracy_score(y_test, predictions) # comparing classifier predcitions to the actual labels
#==============================================================================

# Random forest classifier

random_classifier = RandomForestClassifier(n_estimators=100)#n_jobs=2)#n_estimators=100)
#random_classifier.fit(X_train, y_train)
#predictions = random_classifier.predict(X_test) #testing data

#conf = confusion_matrix(y_test, predictions, labels=[0,1])
#print "Random Forests's Parameters from Sean's code: "        
#accuracy = np.sum(np.diag(conf)) / float(np.sum(conf))
#precision = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=1).astype(float))
#recall = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=0).astype(float))
#print "The accuracy is {}".format(accuracy)
#print "The precision is {}".format(precision)
#print "The recall is {}".format(recall)
#print "Score of Random forest classifier: "
#print accuracy_score(y_test, predictions) # comparing classifier predcitions to the actual labels


#with open('E:\Wearable&MobileSensorComputing\data\classifier.pickle', 'wb') as f: # 'wb' stands for 'write bytes'
#    pickle.dump(random_classifier, f)

#==============================================================================
# print my_classifier.predict(new_data_up)
# print clf.predict(new_data_up)
# print random_classifier.predict(new_data_up)
# 
# print my_classifier.predict(new_data_down)
# print clf.predict(new_data_down)
# print random_classifier.predict(new_data_down)                
#==============================================================================
#==============================================================================
n = len(y)
cv = cross_validation.KFold(n, n_folds=10, shuffle=True, random_state=None)
total_accuracy = 0.0
total_precision = [0.0, 0.0]
total_recall = [0.0, 0.0]
for i, (train_indexes, test_indexes) in enumerate(cv):
     X_train = X[train_indexes, :]
     y_train = y[train_indexes]
     X_test = X[test_indexes, :]
     y_test = y[test_indexes]
     
     print("Fold {} : Training Random forest classifier over {} points...".format(i, len(y_train)))
     sys.stdout.flush()
     random_classifier.fit(X_train, y_train)
#     
     print("Evaluating classifier over {} points...".format(len(y_test)))
#     # predict the labels on the test data
     y_pred = random_classifier.predict(X_test)
# 
#     # show the comparison between the predicted and ground-truth labels
     conf = confusion_matrix(y_test, y_pred, labels=[0,1])
     print conf
     accuracy = np.sum(np.diag(conf)) / float(np.sum(conf))
     precision = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=1).astype(float))
     recall = np.nan_to_num(np.diag(conf) / np.sum(conf, axis=0).astype(float))
#     
     total_accuracy += accuracy
     total_precision += precision
     total_recall += recall
#     
#     print("The accuracy is {}".format(accuracy))  
#     print("The precision is {}".format(precision))    
#     print("The recall is {}".format(recall))
#     
#     print("\n")
     sys.stdout.flush()
print("The average accuracy is {}".format(total_accuracy/10.0))  
print("The average precision is {}".format(total_precision/10.0))    
print("The average recall is {}".format(total_recall/10.0))  
#==============================================================================
with open('E:\Wearable&MobileSensorComputing\data\classifier.pickle', 'wb') as f: # 'wb' stands for 'write bytes'
    pickle.dump(random_classifier, f)