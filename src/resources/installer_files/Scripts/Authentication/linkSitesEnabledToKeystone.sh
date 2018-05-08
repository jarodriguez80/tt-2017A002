#!/bin/bash
rm -f /etc/apache2/sites-enabled/wsgi-keystone.conf
ln -s /etc/apache2/sites-available/wsgi-keystone.conf /etc/apache2/sites-enabled