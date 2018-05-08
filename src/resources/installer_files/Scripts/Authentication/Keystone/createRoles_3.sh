#!/bin/bash

# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

# For create Admin Role
openstack role create admin
# For create User Role
openstack role create user