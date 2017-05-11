# GestureMusic

## Description
GestureMusic is an Android music player that is controlled by hand gestures detected from a Microsoft Band worn by the user. Accelerometer 
and gyroscope sensor data is used.

## Components
1. Microsoft Band – Worn by the user. In my implementation, I am assuming the user is wearing the band on his left hand.
2. GestureMusic – Music player application running on an Android phone.
3. Server – Server responsible for the bi-directional communication of data.
4. Laptop – Running the Python scripts.

## Structure of GestureMusic

## Gesture Recognition Pipeline
1. Sensing Layer
* Window of 25 sensor data points
2. Segmentation Layer
3. Feature Extraction Layer
* Statistical features for each axis (Mean, max, min, median, peak_to_peak, variance)
* Distance from differnece of start and peal/valley indexes
* In total 34 features
4. Classification
* Used Random Forest Classifer with Accuracy 99.6%, Precison 0.991 and Recall 0.994

## Gestures currently supported
1. Volume up
2. Volume down
3. Play/pause
4. Next song
5. Previous song
