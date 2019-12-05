#!usr/bin/python
# -*- coding: utf-8 -*-
__author__ = "Samuel Steinberg"
__date__ = "November 21st, 2019"
'''
    DESCRIPTION: This program decodes an image and returns the data encoded.
    USAGE: Run from the command line. Examples:
            # python decode.py --image IMAGE_NAME.png
'''
import argparse
from pyzbar.pyzbar import decode
from PIL import Image

ap = argparse.ArgumentParser()
ap.add_argument("-i", "--image", required=True, help="provide image path here")
args = vars(ap.parse_args())
results = decode(Image.open(args['image']))
decoded_data = (results[0].data).decode('utf-8')
print(decoded_data)
