#!/bin/bash

# According to https://fedoraproject.org/wiki/Packaging:Scriptlets#Syntax
# The value passed in $1 is
# 0 for uninstall
# 1 for upgrade
#
# In the case of uninstall we need
# Remove the application cron file
if [ "$1" = "0" ]; then
    if [ -r /etc/cron.d/ddc.TOR ] ; then
        /bin/rm -f /etc/cron.d/ddc.TOR
    fi
fi

exit 0
