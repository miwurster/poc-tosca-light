#!/bin/sh
virtualenv env
source env/bin/activate
pip install -r requirements.txt
python app.py