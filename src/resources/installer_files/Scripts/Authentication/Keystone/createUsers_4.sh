#!/bin/bash
# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

openstack user create --domain default --password adminPassword admin
openstack user create --domain default --password swiftPassword swift
openstack user create --domain default --password demo demo