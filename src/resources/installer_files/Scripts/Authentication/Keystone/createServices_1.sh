#!/bin/bash

# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

# For create Identity Service && Object Storage Service
openstack service create --name keystone --description "OpenStack Identity" identity && openstack service create --name swift --description "OpenStack Object Storage" object-store