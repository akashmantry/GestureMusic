# -*- coding: utf-8 -*-
"""
Created on Mon Apr 10 16:57:13 2017

@author: Akash
"""
import numpy as np
import math
import csv

# Used when calulcating distance using double integration
#==============================================================================
# def compute_magnitude_1(a):
#     magn_array = []
#     for i, val in enumerate(a):
#         magn_array.append(math.sqrt(pow(a[i], 2)))
#     return sum(magn_array)
# 
# def compute_magnitude_3(a, b, c):
#     magn_array = []
#     for i, val in enumerate(a):
#         magn_array.append(math.sqrt(pow(a[i], 2) + pow(b[i], 2) + pow(c[i], 2)))
#     return sum(magn_array)
# 
# def compute_magnitude_2(a, b):
#     magn_array = []
#     for i, val in enumerate(a):
#         magn_array.append(math.sqrt(pow(a[i], 2) + pow(b[i], 2)))
#     return sum(magn_array)
#==============================================================================


def compute_features(input):
    #print "Inside compute features"
    array_ax = input[0]
    array_ay = input[1]
    array_az = input[2]
    
    array_avx = input[3]
    array_avy = input[4]
    array_avz = input[5]
    
    distance = input[6]
    
    #vx = input[6]
    #vy = input[7]
    #vz = input[8]
    
    #PX = input[9]
    #PY = input[10]
    #PZ = input[11]
    
    a_x_bundle = feature_bundle(array_ax)
    a_y_bundle = feature_bundle(array_ay)
    a_z_bundle = feature_bundle(array_az)
    
    a_vx_bundle = feature_bundle(array_avx)
    a_vy_bundle = feature_bundle(array_avy)
    a_vz_bundle = feature_bundle(array_avz)
    
    a_vx_bundle = a_vx_bundle[0:-1]
    a_vy_bundle = a_vy_bundle[0:-1]
    a_vz_bundle = a_vz_bundle[0:-1]
    
    #vx_bundle = feature_bundle(vx)
    #vy_bundle = feature_bundle(vy)
    #vz_bundle = feature_bundle(vz)
    
    #vx_bundle = vx_bundle[0:3]
    #vy_bundle = vy_bundle[0:3]
    #vz_bundle = vz_bundle[0:3]
    
    #distance = compute_magnitude_1(PX)
    #distance_a_y = compute_magnitude_1(PY)
    #distance_a_z = compute_magnitude_1(PZ)
    #distance_combined = compute_magnitude_3(PX, PY, PZ)
    
    new_list = []
    append_to_list(a_x_bundle, new_list)
    append_to_list(a_y_bundle, new_list)
    append_to_list(a_z_bundle, new_list)
    append_to_list(a_vx_bundle, new_list)
    append_to_list(a_vy_bundle, new_list)
    append_to_list(a_vz_bundle, new_list)
    #append_to_list(vx_bundle, new_list)
    #append_to_list(vy_bundle, new_list)
    #append_to_list(vz_bundle, new_list)
    new_list.append(distance)
    #new_list.append(distance_a_y)
    #new_list.append(distance_a_z)
    #new_list.append(distance_combined)
    
    #comment while testing
    return new_list
    
    #uncomment while testing
    #write_to_file(new_list)
    #print "Features written to file"

def write_to_file(new_list):
    fo_new = open("E:\Wearable&MobileSensorComputing\data\data_new_next_previous_features.csv", "ab")
    writer = csv.writer(fo_new, delimiter = ",")
    writer.writerow(new_list)
    fo_new.close()
    
def feature_bundle(var):
    var_bundle = []
    var_max = np.amax(var, axis = 0)
    var_mean = np.mean(var, axis = 0)
    var_var = np.var(var, axis = 0)
    var_min = np.amin(var, axis = 0)
    var_median = np.median(var, axis = 0)
    var_peak_to_peak = var_max - var_min
    var_bundle.extend([var_max, var_mean, var_var, var_min, var_median, var_peak_to_peak])
    return var_bundle

def append_to_list(old_list, new_list):
    for i in old_list:
        new_list.append(i)
    return new_list
