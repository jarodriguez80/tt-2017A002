#!/bin/bash
# For the authentication node
service apache2 restart
service mysql restart
service memcached restart
service keystone restart

# For  the storage node
service rsync start