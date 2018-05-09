#!/bin/sh
echo "" > /dev/null
export OS_TOKEN=tokenLoko && echo "tokenSet=OK" >> ~/out
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3 && echo "urlSet=OK" >> ~/out
export OS_IDENTITY_API_VERSION=3 && echo "apiVersionSet=OK" >> ~/out
# For create Identity Service
echo "" > /dev/null
openstack service create --name keystone --description "OpenStack Identity" identity && echo "identityServiceCreation=OK" >> ~/out
# For create Identity Endpoints
openstack endpoint create --region RegionOne identity public http://ipAddressForAuthenticationNode?:portForPublicRequests?/v3 && echo "identityPublicEndpointCreated=OK" >> ~/out
openstack endpoint create --region RegionOne identity internal http://ipAddressForAuthenticationNode?:portForPublicRequests?/v3 && echo "identityInternalEndpointCreated=OK" >> ~/out
openstack endpoint create --region RegionOne identity admin http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3 && echo "identityAdminEndpointCreated=OK" >> ~/out
# For create Default Domain
openstack domain create --description "Default Domain" default && echo "defaultDomainCreated=OK" >> ~/out
# For create Admin Project, Admin User & Admin Role
openstack project create --domain default --description "Admin Project" admin && echo "adminProjectCreated=OK" >> ~/out
openstack user create --domain default --password adminPassword admin && echo "adminUserCreated=OK" >> ~/out
openstack role create admin && echo "adminRoleCreated=OK" >> ~/out
# Link Admin Role and Admin Project to the Admin User
openstack role add --project admin --user admin admin && echo "linkAdminUserWithAdminRoleAndAdminProject=OK" >> ~/out
# For create Service Project
openstack project create --domain default --description "Service Project" service && echo "serviceProjectCreated=OK" >> ~/out
# For create Swift User
openstack user create --domain default --password swiftPassword swift && echo "swiftUserCreated=OK" >> ~/out
# Link Admin Role and Service Project to Swift User
openstack role add --project service --user swift admin && echo "linkSwiftUserWithAdminRoleAnServiceProject=OK" >> ~/out
# For create User Role
openstack role create user && echo "userRoleCreated=OK" >> ~/out
# For create  Object Storage Service
openstack service create --name swift --description "OpenStack Object Storage" object-store && echo "objectStorageServiceCreation=OK" >> ~/out
# For create Object Storage Endpoints
openstack endpoint create --region RegionOne object-store public http://ipAddressForAuthenticationNode?:portForSwiftRequests?/v1/AUTH_%\(tenant_id\)s && echo "storagePublicEndpointCreated=OK" >> ~/out
openstack endpoint create --region RegionOne object-store internal http://ipAddressForAuthenticationNode?:portForSwiftRequests?/v1/AUTH_%\(tenant_id\)s && echo "storageInternalEndpointCreated=OK" >> ~/out
openstack endpoint create --region RegionOne object-store admin http://ipAddressForAuthenticationNode?:portForSwiftRequests?/v1 && echo "storageAdminEndpointCreated=OK" >> ~/out
# For create Demo Project
openstack project create --domain default --description "Demo Project" demo && echo "demoProjectCreation=OK" >> ~/out
# For create Demo User
openstack user create --domain default --password demo demo && echo "demoUserCreated=OK" >> ~/out
# Link Demo User and Demo Project to Demo User
openstack role add --project demo --user demo user && echo "linkDemoUserWithDemoProjectAndUserRole=OK" >> ~/out