# -*- coding: utf-8 -*-
"""
Created on Wed May 10 00:06:40 2017

@author: Akash
"""


from low_pass_filter import low_pass
from mv_detect_peaks import detect_peaks
from sklearn import preprocessing
from scipy.signal import butter, filtfilt
from scipy import integrate
from features import compute_features
import numpy as np
#from smooth import smooth
from test_classifier import predict

class segmentation:
    
    previous_av_x_peak = False # keeps track if we had a previous av_x peak
    start_appending = False
    av_x_peak_detected = False
    a_x_peak_detected = False
    time_start = 0
    time_end = 0
    candidate_buffer = []
    array_a_x_peaks = []
    a_x_max_peak = 0
    peak_detected = -1
    previous_value_sent = 0

    # Convert the range of a_x acceleration to 0:1 for peak/valley detection required for calculating height
    @staticmethod
    def preprocess(array, peak_detected):
        data_scaler = preprocessing.MinMaxScaler(feature_range=(0, 1))
        a = np.array(array).reshape(-1,1)
        data_scaled_acc = data_scaler.fit_transform(a)
        b = np.concatenate( data_scaled_acc, axis=0 )
        detected_peak_indexes = []
        detected_peak_indexes = detect_peaks(b, mph=0, mpd=len(b), edge="rising", valley=True, show=False)
        return detected_peak_indexes
    
    # Used for monitoring increase in a_x acceleration - 5 strikes allowed 
    @staticmethod    
    def increasing(L):
        counter = 0
        for i in range(0, len(L) - 1):
            if L[i] > L[i+1]:
                counter += 1
        if counter < 5:
            return True
        return False
    
    # First-part of segmentation layer
    # Detect valleys in a_x acceleration and a_y acceleration
    # Start timer
    # Detect a_x acceleration zero-crossing or continous increase
    # Stop timer
    # Pass data to next part
    @staticmethod
    def get_candidate_segments(temp):
         #print "Inside get_candidate block"
         time = temp[0]
         a_x_output = temp[1]
         a_y_output = temp[2]
         a_z_output = temp[3]
         av_x_output = temp[4]
         av_y_output = temp[5]
         av_z_output = temp[6]
         new_list = []
         start_index = 0
         
         
         if segmentation.previous_av_x_peak == False:
             output_av_x_up = detect_peaks(av_x_output, mph=100, mpd=25, valley = True, show=False)
             output_av_y_down = detect_peaks(av_y_output, mph=50, mpd=25, valley = True, show=False)
             #output_av_z_down = detect_peaks(av_z_output, mph=30, mpd=25, valley = True, show=False)
             #output_a_x = detect_peaks(a_x_output, mph=0, mpd=25, edge='rising', show=True)
             
             
             if output_av_y_down or output_av_x_up:
                 
                 if output_av_x_up:
                     print "Detected av_x peak"
                     segmentation.peak_detected = 0 
                     start_index = output_av_x_up[0]
                     segmentation.time_start =  time[start_index]
                     
                 elif output_av_y_down: #and output_av_z_down:
                     print "Detected av_y peak"
                     segmentation.peak_detected = 1
                     start_index = output_av_y_down[0]
                     segmentation.time_start =  time[start_index]    
                 
  
                 segmentation.time_end = segmentation.time_start + 2000
                 segmentation.previous_av_x_peak = True
                 segmentation.start_appending = True
                 segmentation.av_x_peak_detected = True
         
         # detect zero-crossing or increase in a_x
         if segmentation.start_appending:
            output_a_x = detect_peaks(a_x_output, mph=0, mpd=25, edge='rising', show=False)
            is_increasing = segmentation.increasing(a_x_output)

            if output_a_x or is_increasing:
                segmentation.a_x_peak_detected = True #a peak
                
            if segmentation.av_x_peak_detected == False: #av peak
                if time[-1] < segmentation.time_end:
                    segmentation.candidate_buffer.extend([time[i], a_x_output[i], a_y_output[i], a_z_output[i], av_x_output[i], av_y_output[i], av_z_output[i]] for i in range(len(time)))        
                elif time[-1] > segmentation.time_end and segmentation.a_x_peak_detected: #just for testing

                    try:
                        index = time.index(segmentation.time_end)
                    except ValueError:
                        segmentation.candidate_buffer.extend([time[i], a_x_output[i], a_y_output[i], a_z_output[i], av_x_output[i], av_y_output[i], av_z_output[i]] for i in range(len(time)))
                    else:
                        segmentation.candidate_buffer.extend([time[i], a_x_output[i], a_y_output[i], a_z_output[i], av_x_output[i], av_y_output[i], av_z_output[i]] for i in range(index))
                    
                    segmentation.previous_av_x_peak = False
                    segmentation.start_appending = False
                    segmentation.a_x_peak_detected = False
                    segmentation.time_start = 0
                    segmentation.time_end = 0
                    new_list = [segmentation.peak_detected, segmentation.candidate_buffer[:]]
                    segmentation.peak_detected = -1
                    del segmentation.candidate_buffer[:]
                    return new_list
            else:
               segmentation.candidate_buffer.extend([time[i], a_x_output[i], a_y_output[i], a_z_output[i], av_x_output[i], av_y_output[i], av_z_output[i]] for i in range(start_index, len(time)))
               segmentation.av_x_peak_detected = False
         
         return None
     

    # Second-part of segmentation layer
    # Detect x-axis acceleration peaks for distance
    # Pass data to feature extraction
    @staticmethod
    def find_peaks(data):
        print "Inside find_a_x peak function"
        peak_detected = data[0]

        temp_data = data[1]
        temp_list = []
        distance_index = 0
        time_taken = 0
        
        time = []
        array_ax = []
        array_ay = []
        array_az = []
        array_avx = []
        array_avy = []
        array_avz = []
        
        
        for i in range(len(temp_data)):
            time.append(temp_data[i][0])
            array_ax.append(temp_data[i][1])
            array_ay.append(temp_data[i][2])
            array_az.append(temp_data[i][3])
            array_avx.append(temp_data[i][4])
            array_avy.append(temp_data[i][5])
            array_avz.append(temp_data[i][6])
        
        
        output_a_x = segmentation.preprocess(array_ax, peak_detected)
         
        if peak_detected == 1:
            #output_a_x = detect_peaks(array_ax, mph=0.2, mpd=25, valley=True, show=True)
            #output_a_x = detect_peaks(array_ax, threshold=0.5, mpd=25, valley=True, show=True)
            #a = detect_peaks(array_ax, threshold=0.5, mpd=25, edge='falling', show=True)
            if np.any(output_a_x):
                for i in output_a_x:
                    temp_list.append(array_ax[i])
                minimum_peak = min(temp_list)##min(output_a_x)
                distance_index = array_ax.index(minimum_peak)
                distance_index = distance_index/2
                
                print "end_index: " + str(distance_index)
            else:
                return None
            
        elif peak_detected == 0:
            #detect_peaks(array_ax, mph=0.2, mpd=25, edge='rising', show=True)
            #output_a_x = detect_peaks(array_ax, threshold=0.5, mpd=25, edge='rising', show=False)
            if np.any(output_a_x):
                for i in output_a_x:
                    temp_list.append(array_ax[i])
                maximum_peak = max(temp_list)
                distance_index = array_ax.index(maximum_peak)#minimum_peak
                if np.amin(array_ax[distance_index:], axis = 0) > 0.2: 
                    return None
            else:
                return None
            
        # If using double integration
        
        #print "a_x peaks detected"
        #output_temp = [-x for x in array_ax]
        #detect_peaks(output_temp, mph=0.2, mpd=25, edge='rising', show=True)
        
        #array_ax = array_ax[:distance_index]
        #print len(array_ax)
        
        
        
        #detect_peaks(array_avx, mph=200, mpd=25, edge='falling', show=True)
        
        #try:
            #passing acceleration through high pass filter to remove gravity
            #[b,a] = butter (5, 0.017, 'high')
            #a_x_high_output = filtfilt (b, a, np.asarray(array_ax), padtype = None)
            #a_y_high_output = filtfilt (b, a, np.asarray(array_ay), padtype = None)
            #a_z_high_output = filtfilt (b, a, np.asarray(array_az), padtype = None)
            
            #sample_rate = 25
            
            #obtaining velocity from acceleration
            #vx = integrate.cumtrapz(a_x_high_output/sample_rate)
            #vy = integrate.cumtrapz(a_y_high_output/sample_rate)
            #vz = integrate.cumtrapz(a_z_high_output/sample_rate)
            
            #passing velocity through high pass filter
            #FVX = filtfilt (b, a, np.asarray(vx), padtype = None);
            #FVY = filtfilt (b, a, np.asarray(vy), padtype = None);
            #FVZ = filtfilt (b, a, np.asarray(vz), padtype = None);
            
                       
            #obtaining diplacement  from velocity
            #PX = integrate.cumtrapz (FVX/sample_rate);
            #PY = integrate.cumtrapz (FVY/sample_rate);
            #PZ = integrate.cumtrapz (FVZ/sample_rate);
                                    
        time_taken = time[distance_index] - time[0]
        if time_taken <= 0:
            return
            #print "Time_taken: " + str(time_taken)
                                    
        #except ValueError:
        #except Exception, e:
        #    print str(e)
        #    pass
        #else:
        #computing magnitude of distance - not doing with just because it produces wrong results
        #distance = compute_magnitude(PX, PY, PZ)
        #print "Passing features to feature module"
            #output = [array_ax, array_ay, array_az, array_avx, array_avy, array_avz, vx, vy, vz, PX, PY, PZ]
        output = [array_ax, array_ay, array_az, array_avx, array_avy, array_avz, time_taken]
        return output
        #return
        #compute_features(output)
         
    
        
    #==============================================================================
    @staticmethod
    def segment(sensor_data, send_notification):
         time = []
         array_ax = []
         array_ay = []
         array_az = []
         array_avx = []
         array_avy = []
         array_avz = []
         
         output = []
         output_candidate_segment = []
         output_feature_extracted = []
         output_peaks = []
         
         #parameters being passed through low-pass filter to smoothen the plots
         for i in range(sensor_data[0].size):
             time.append(sensor_data[i][0])
             array_ax.append(sensor_data[i][1])
             array_ay.append(sensor_data[i][2])
             array_az.append(sensor_data[i][3])
             array_avx.append(sensor_data[i][4])
             array_avy.append(sensor_data[i][5])
             array_avz.append(sensor_data[i][6])
         
         #passing through a low-ass filter    
         a_x_output = low_pass(array_ax)
         a_y_output = low_pass(array_ay)
         a_z_output = low_pass(array_az)
         av_x_output = low_pass(array_avx)
         av_y_output = low_pass(array_avy)
         av_z_output = low_pass(array_avz)
         
         temp = [time, a_x_output, a_y_output, a_z_output, av_x_output, av_y_output, av_z_output]
         
         output_candidate_segment = segmentation.get_candidate_segments(temp)
         if output_candidate_segment:
             output_peaks = segmentation.find_peaks(output_candidate_segment)
         if output_peaks:
             output_feature_extracted = compute_features(output_peaks)
         
            # comment while testing
         if output_feature_extracted:
             output = predict(output_feature_extracted)
             
             #if the old value sent was same, ignore - used for threads as they generate same values
             if segmentation.previous_value_sent == output[0]:
                 return
             
             segmentation.previous_value_sent = output[0]
             print output
             json = {output[1] : output[0]}
             send_notification("GESTURE_DETECTED", json)
             print "Notification sent"
         return
        