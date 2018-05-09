#!/bin/bash
chown -R swift:swift /srv/node
chown -R root:swift /var/cache/swift
chmod -R 775 /var/cache/swift
chown -R root:swift /etc/swift