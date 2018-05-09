#!/bin/bash
dpkg-reconfigure -f noninteractive tzdata
echo "mysql-server-5.6 mysql-server/root_password password root" | sudo debconf-set-selections
echo "mysql-server-5.6 mysql-server/root_password_again password root" | sudo debconf-set-selections
apt-get install mysql-server-5.6 -y
apt-get install python-pymysql -y