#!/bin/bash
echo "manual" > /etc/init/keystone.override
apt-get install keystone apache2 libapache2-mod-wsgi -y