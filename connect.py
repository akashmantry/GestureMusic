# -*- coding: utf-8 -*-
"""
Created on Mon Apr 10 14:39:57 2017

@author: Akash
"""

import socket
import sys
import json
import threading
from latest_segmentation import segmentation
import numpy as np

msg_request_id = "ID"
msg_authenticate = "ID,{}\n"
msg_acknowledge_id = "ACK"


a_data = {}
sensor_data = []
window_size = 25

class Client:
    def __init__(self, user_id, disconnect_callback=None):
        self.user_id = user_id
        
        # establish the connection for sending data to the server 
        self.send_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.send_socket.connect(("none.cs.umass.edu", 9999))
        
        # establish the connection for receiving data from the server
        self.receive_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.receive_socket.connect(("none.cs.umass.edu", 8888))
        
        # ensures that after 1 second, a keyboard interrupt will close
        self.receive_socket.settimeout(1.0)
        
        # maps a data type, e.g. "SENSOR_ACCEL" for accelerometer data, to a function for analytics
        self.function_mapping = {}
        
        self.disconnect_callback = disconnect_callback
        
    def connect(self):
            """
            Establishes both incoming (receive) and outgoing (send) connections to the server 
            and authenticates the user.
            """
            try:
                print("Authenticating user for receiving data...")
                sys.stdout.flush()
                self._authenticate(self.receive_socket)
                
                print("Authenticating user for sending data...")
                sys.stdout.flush()
                self._authenticate(self.send_socket)
                
                print("Successfully connected to the server! Waiting for incoming data...")
                sys.stdout.flush()
                    
                previous_json = ''
                    
                while True:
                    try:
                        message = self.receive_socket.recv(1024).strip()
                        json_strings = message.split("\n")
                        json_strings[0] = previous_json + json_strings[0]
                        for json_string in json_strings:
                            try:
                                data = json.loads(json_string)
                                
                            except:
                                previous_json = json_string
                                continue
                            previous_json = '' # reset if all were successful
                            sensor_type = data['sensor_type']
                            if sensor_type == "SENSOR_ACCEL":
                                temp = data['data']
                                t = temp['t']
                                a_x = temp['a_x']
                                a_y = temp['a_y']
                                a_z = temp['a_z']
                                av_x = temp['av_x']
                                av_y = temp['av_y']
                                av_z = temp['av_z']
                                
                                sensor_data.append((t,a_x,a_y,a_z,av_x,av_y,av_z))
                                while len(sensor_data) > window_size:
                                    sensor_data.pop(0)
                                
                                if (len(sensor_data) == window_size):
                                    t = threading.Thread(target=segmentation.segment,  args=(np.asarray(sensor_data[:]), self.send_notification))
                                    t.start()

                            
                        sys.stdout.flush()
                    except KeyboardInterrupt: 
                        # occurs when the user presses Ctrl-C
                        print("User Interrupt. Quitting...")
                        break
                    except Exception as e:
                        # ignore exceptions, such as parsing the json
                        # if a connection timeout occurs, also ignore and try again. Use Ctrl-C to stop
                        # but make sure the error is displayed so we know what's going on
                        if (e.message != "timed out"):  # ignore timeout exceptions completely       
                            print(e)
                        else:
                            previous_json=''
                        pass
            except KeyboardInterrupt: 
                # occurs when the user presses Ctrl-C
                print("User Interrupt. Quitting...")
            finally:
                
                print >>sys.stderr, 'closing socket for receiving data'
                self.receive_socket.shutdown(socket.SHUT_RDWR)
                self.receive_socket.close()
                
                print >>sys.stderr, 'closing socket for sending data'
                self.send_socket.shutdown(socket.SHUT_RDWR)
                self.send_socket.close()
                
                if self.disconnect_callback != None:
                    self.disconnect_callback()
            
    def _authenticate(self, sock):
        """
        Authenticates the user by performing a handshake with the data collection server.
        
        If it fails, it will raise an appropriate exception.
        """
        message = sock.recv(256).strip()
        if (message == msg_request_id):
            print("Received authentication request from the server. Sending authentication credentials...")
            sys.stdout.flush()
        else:
            print("Authentication failed!")
            raise Exception("Expected message {} from server, received {}".format(msg_request_id, message))
        sock.send(msg_authenticate.format(self.user_id))
    
        try:
            message = sock.recv(256).strip()
        except:
            print("Authentication failed!")
            raise Exception("Wait timed out. Failed to receive authentication response from server.")
            
        if (message.startswith(msg_acknowledge_id)):
            ack_id = message.split(",")[1]
        else:
            print("Authentication failed!")
            raise Exception("Expected message with prefix '{}' from server, received {}".format(msg_acknowledge_id, message))
        
        if (ack_id == self.user_id):
            print("Authentication successful.")
            sys.stdout.flush()
        else:
            print("Authentication failed!")
            raise Exception("Authentication failed : Expected user ID '{}' from server, received '{}'".format(self.user_id, ack_id))
            
    def send_notification(self, message, data):
        """
        Sends a notification back over the server. The message parameter identifies the 
        message, e.g. "STEP_DETECTED". The data parameter is optional and includes 
        any additional data, e.g. the timestamp at which the step occurred, that 
        could be relevant. The data must be valid JSON.
        """
        self.send_socket.send(json.dumps({'user_id' : self.user_id, 'sensor_type' : 'SENSOR_SERVER_MESSAGE', 'message' : message, 'data': data}) + "\n")
        
    def set_disconnect_callback(self, disconnect_callback):
        """
        Sets a callback function to be called when the server-client connection ends
        """
        self.disconnect_callback = disconnect_callback







 
    



        
