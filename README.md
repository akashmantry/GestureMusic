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
![Player](/Images/structure.PNG?raw=true)

## Gesture Recognition Pipeline
1. Sensing Layer
* Window of 25 sensor data points
2. Segmentation Layer
![Player1](/Images/segment.PNG?raw=true)
3. Feature Extraction Layer
* Statistical features for each axis (Mean, max, min, median, peak_to_peak, variance)
* Distance from differnece of start and peak/valley indexes
* In total 34 features
4. Classification
* Used Random Forest Classifer

## Gestures currently supported
1. Volume up
![Player2](/Images/combined_colume_up.PNG?raw=true)
![Player3](/Images/volume-up.PNG?raw=true)
2. Volume down
![Player3](/Images/combined_volume_down.PNG?raw=true)
![Player4](/Images/volume-down.PNG?raw=true)
3. Play/pause
![Player5](/Images/combined_clap.PNG?raw=true)
![Player6](/Images/clap.PNG?raw=true)
4. Next song
![Player7](/Images/combined_next.PNG?raw=true)
![Player8](/Images/next.PNG?raw=true)
5. Previous song
![Player9](/Images/combined_previous.PNG?raw=true)
![Player10](/Images/previous.PNG?raw=true)
