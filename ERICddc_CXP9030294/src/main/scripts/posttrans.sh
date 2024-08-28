#posttrans scriptlet (using /bin/sh):

#
# If ddc is still installed at the end of the transaction
# start the service
#
ddcService() {
    ACTION=$1
    if [ -x /etc/init.d/ddc ] ; then
        if [ $(/sbin/pidof systemd) ] ; then
            /bin/systemctl $ACTION ddc.service
        elif [ $(/sbin/pidof init) ] ; then
            /sbin/service ddc $ACTION
        else
            echo "Error: Failed to find any services system."
        fi
    fi
}

# TORF-473617, Removing /var/tmp/ddc_data because this is a enm environment
if [ -d "/var/tmp/ddc_data" ] ; then
    ddcService stop
    echo "Info: Removing /var/tmp/ddc_data on enm environment"
    rm -rf /var/tmp/ddc_data
fi

ddcService start
