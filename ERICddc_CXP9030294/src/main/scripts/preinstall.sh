#!/bin/bash

# According to https://fedoraproject.org/wiki/Packaging:Scriptlets#Scriptlet_Ordering
# This will be the first scriptlet to be run during an install or upgrade

# According to https://fedoraproject.org/wiki/Packaging:Scriptlets#Syntax
# The value passed in $1 is
# 1 for install
# 2 for upgrade
# In the case of upgrade we want to stop the any running ddc service
# before we do the upgrade (as the new software may have a different
# way of shutting down)
if [ "$1" -gt "1" ] ; then
    if [ $(/sbin/pidof systemd) ] ; then
        /bin/systemctl stop ddc.service
    elif [ $(/sbin/pidof init) ] ; then
        /sbin/service ddc stop
    else
        echo "Error: Failed to find any services system."
    fi
fi

exit 0
