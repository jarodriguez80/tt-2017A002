#!/bin/bash
#######
export OS_TOKEN=tokenLoko && echo "tokenSet=OK" >> ~/outTwo
export OS_URL=http://ipAddressForAuthenticationNode?:portForAdminRequests?/v3 && echo "urlSet=OK" >> ~/outTwo
export OS_IDENTITY_API_VERSION=3 && echo "apiVersionSet=OK" >> ~/outTwo
#######
echo "" > /dev/null
echo "" > /dev/null
echo "" > /dev/null
echo "" > /dev/null
echo "" > /dev/null
echo "" > /dev/null
echo "" > /dev/null
openstack --log-file ~/log -v service create --name keystone --description "OpenStack Identity" identity && echo "identityServiceCreation=FAIL" >> ~/outTwo
openstack --log-file ~/log -v service create --name swift --description "OpenStack Object Storage" object-store || echo "objectStorageServiceCreation=FAIL" >> ~/out

