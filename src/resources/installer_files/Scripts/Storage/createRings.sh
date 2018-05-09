#!/bin/bash

# $1 ipAddressForStorageNode 
# $2 portForSwiftAccountRequests
# $3 portForSwiftContainerRequests
# $4 portForSwiftObjectRequests
# Sample of use
# ./createRings ipAddress deviceName 6002 6001 6000

cd /etc/swift
# Create Account Ring
swift-ring-builder /etc/swift/account.builder create 10 3 1 
swift-ring-builder /etc/swift/account.builder add --region 1 --zone 1 --ip $1 --port $3 --device $2 --weight 100
swift-ring-builder /etc/swift/account.builder set_replicas 1 
swift-ring-builder /etc/swift/account.builder rebalance 

# Create Container Ring
swift-ring-builder /etc/swift/container.builder create 10 3 1 
swift-ring-builder /etc/swift/container.builder add --region 1 --zone 1 --ip $1 --port $4 --device $2 --weight 100
swift-ring-builder /etc/swift/container.builder set_replicas 1 
swift-ring-builder /etc/swift/container.builder rebalance 

# Create Object Ring
swift-ring-builder /etc/swift/object.builder create 10 3 1 
swift-ring-builder /etc/swift/object.builder add --region 1 --zone 1 --ip $1 --port $5 --device $2 --weight 100
swift-ring-builder /etc/swift/object.builder set_replicas 1 
swift-ring-builder /etc/swift/object.builder rebalance 
