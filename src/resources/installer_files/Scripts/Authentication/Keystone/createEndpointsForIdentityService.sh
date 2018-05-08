#!/bin/bash

# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

# For create Identity Endpoints
openstack endpoint create --region RegionOne identity public http://ipAddressForAuthenticationNode?:portForPublicRequests?/v3
openstack endpoint create --region RegionOne identity internal http://ipAddressForAuthenticationNode?:portForPublicRequests?/v3
openstack endpoint create --region RegionOne identity admin http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3