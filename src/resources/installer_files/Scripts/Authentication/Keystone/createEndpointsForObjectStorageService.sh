#!/bin/bash

# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

# For create Object Storage Endpoints
openstack endpoint create --region RegionOne object-store public http://ipAddressForAuthenticationNode?:portForSwiftRequests?/v1/AUTH_%\(tenant_id\)s
openstack endpoint create --region RegionOne object-store internal http://ipAddressForAuthenticationNode?:portForSwiftRequests?/v1/AUTH_%\(tenant_id\)s
openstack endpoint create --region RegionOne object-store admin http://ipAddressForAuthenticationNode?:portForSwiftRequests?/v1