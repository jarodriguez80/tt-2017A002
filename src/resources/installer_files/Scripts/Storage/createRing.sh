#!/bin/bash

cd /etc/swift

####################################################################################################################################
#
#   Create Ring file for each server [Account|Container|Object]
#
#   Creates a RingType with
#   2^<part_power> partitions and <replicas>.
#   <min_part_hours> is number of hours to restrict moving a partition more than once.
#
####################################################################################################################################
#
#   Parameters expected:
#
#   $1 -> RingType [/etc/swift/account.builder | /etc/swift/container.builder | /etc/swift/object.builder]
#   $2 -> <part_power>
#   $3 -> <replicas>
#   $4 -> <min_part_hours>
#   Sample of raw use:
#
#       swift-ring-builder /etc/swift/account.builder create 10 3 1
#
#   Sample of use:
#
#       createRing.sh [/etc/swift/account.builder | /etc/swift/container.builder | /etc/swift/object.builder] 10 3 1
#
####################################################################################################################################

swift-ring-builder $1 create $2 $3 $4
