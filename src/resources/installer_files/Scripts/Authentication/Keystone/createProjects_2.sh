#!/bin/bash

# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

# For create Admin Project
openstack project create --domain default --description "Admin Project" admin
# For create Service Project
openstack project create --domain default --description "Service Project" service
# For create Demo Project
openstack project create --domain default --description "Demo Project" demo