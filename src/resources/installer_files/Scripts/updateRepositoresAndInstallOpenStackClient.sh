#!/bin/sh
apt-get install software-properties-common -y
add-apt-repository cloud-archive:mitaka -y
apt-get update && apt-get dist-upgrade -y
apt-get install python-openstackclient -y