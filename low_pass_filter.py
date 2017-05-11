# -*- coding: utf-8 -*-
"""
Created on Sat Mar 25 00:25:25 2017

@author: Akash
"""

#low-pass filter
def low_pass(input_array):
    ALPHA = 0.6#0.15
    output_array = []
    for i in range(len(input_array)):
        if i == 0:
            output_array.append(input_array[0])
            continue
        output_array.append(output_array[i-1] + ALPHA * (input_array[i] - output_array[i-1]))
    return output_array