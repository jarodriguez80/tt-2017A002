#!/bin/bash
# Export required values for temporal admin authentication
export OS_TOKEN=tokenLoko
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3
export OS_IDENTITY_API_VERSION=3

# Link Admin Role and Admin Project to the Admin User
openstack role add --project admin --user admin admin
# Link Admin Role and Service Project to Swift User
openstack role add --project service --user swift admin
# Link Demo User and Demo Project to Demo User
openstack role add --project demo --user demo user