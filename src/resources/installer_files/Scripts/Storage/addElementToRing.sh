#!/bin/bash

####################################################################################################################################
#
#   Add a new element to the Ring
#
####################################################################################################################################
#
#   Parameters expected:
#
#   $1 -> RingType [/etc/swift/account.builder | /etc/swift/container.builder | /etc/swift/object.builder]
#   $2 -> Region
#   $3 -> Zone
#   $4 -> Ip address of the storage node
#   $5 -> Port for the RingType
#   $6 -> Device name [sdb|sdc]
#   $7 -> Weight [100]
#
#   Sample of use:
#
#       addElementToRing.sh /etc/swift/account.builder 1 1 192.168.1.65 6000 sdc 100
#
####################################################################################################################################

swift-ring-builder $1 add --region $2 --zone $3 --ip $4 --port $5 --device $6 --weight $7