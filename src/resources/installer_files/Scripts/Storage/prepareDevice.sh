#!/bin/bash

# Prepare a block for storage.
# $1 : Device to use
# $2 : Mount Point For Device Selected
# Sample of use
# ./prepareDevice /dev/sdb /srv/node/sdb

mkfs.xfs -f $1
mkdir -p $2
echo "$1 $2 xfs noatime,nodiratime,nobarrier,logbufs=8 0 2" >> /etc/fstab
mount $2
