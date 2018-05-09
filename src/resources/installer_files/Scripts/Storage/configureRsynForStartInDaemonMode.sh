#!/bin/bash

# Configure rsync for start in Daemon Mode from init.d Script
sed -i "s/RSYNC_ENABLE=false/RSYNC_ENABLE=true/" /etc/default/rsync