#!usr/bin/python
# -*- coding: utf-8 -*-
__author__ = "Samuel Steinberg"
__date__ = "November 21st, 2019"
'''
    DESCRIPTION: This program encodes data and generates a QR code. Can handle links, messages, and contact card files.
    USAGE: Run from the command line. Examples:
            # python encode.py --data NAME.vcf --name FILENAME
            # python encode.py --data "Strings with spaces must be in quotes" --name FILENAME
'''
import pyqrcode
import argparse
import os
#Add data in form of the link. Use function to create and save the QR code. Scale must be set to 6...
if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument("-d", "--data", required=True, help="provide some data")
    ap.add_argument("-fname", "--name", required=True, help="provide a filename")
    args = vars(ap.parse_args())
    if args['data'][-4: ] == ".vcf":
        s = ""
        with open(args['data'], 'r') as f:
            for line in f.readlines():
                s += line
        redirect = pyqrcode.create(s)
    else: redirect = pyqrcode.create(args['data'])
    redirect.png( str(args['name'] + ".png"), scale=6)
