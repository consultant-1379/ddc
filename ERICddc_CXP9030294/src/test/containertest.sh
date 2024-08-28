#!/bin/bash

ROOT_DIR=$(dirname $0)
ROOT_DIR=$(cd ${ROOT_DIR}/../../.. ; pwd)

fail() {
    local MSG="$1"
    echo "ERROR: Tests failed, ${MSG}"

    find /var/ericsson/ddc_data/ -name 'ddc*.log' -exec cat {} \;

    if [ -d /var/tmp/ddc_data ] ; then
        find /var/tmp/ddc_data/ -name 'ddc*.log' -exec cat {} \;
    fi

    if [ -r /tmp/metrics.txt ] ; then
        cat /tmp/metrics.txt
    fi

    exit 1
}

checkLogContains() {
    local LOG=$(find /var/ericsson/ddc_data/ -name 'ddc*.log')
    egrep --silent "${LOG_CONTAINS}" ${LOG}
    if [ $? -eq 0 ] ; then
        return 0
    else
        return 1
    fi
}

checkInstrXML() {
    if [ -d /var/ericsson/ddc_data/$(hostname)_TOR ] ; then
        local INSTR_DIR=/var/ericsson/ddc_data/$(hostname)_TOR/config/instr
    else
        local INSTR_DIR=/var/tmp/ddc_data/instr
    fi

    if [ -r ${INSTR_DIR}/genjmx_jboss.xml ] && [ -r ${INSTR_DIR}/threadpooljmx_jboss.xml ] && [ -r ${INSTR_DIR}/e2e_jboss.xml ]; then
        return 0
    else
        return 1
    fi
}

checkJseInstrXML() {
    if [ -d /var/ericsson/ddc_data/$(hostname)_TOR ] ; then
        local INSTR_DIR=/var/ericsson/ddc_data/$(hostname)_TOR/config/instr
    else
        local INSTR_DIR=/var/tmp/ddc_data/instr
    fi

    if [ -r ${INSTR_DIR}/genjmx_jse.xml ] && [ -r ${INSTR_DIR}/e2e_jse.xml ]; then
        return 0
    else
        return 1
    fi
}

checkInstrStarted() {
    curl --insecure --max-time 5 --silent --show-error http://${INSTR_IP}:6789/healthz > /dev/null
    return $?
}

waitFor() {
    local CHECK_FUNCTION=$1
    local DESCRIPTION="$2"

    local MAX_COUNT=10
    local READY=0
    local COUNT=1
    while [ ${READY} -eq 0 ] && [ ${COUNT} -lt ${MAX_COUNT} ]; do
        ${CHECK_FUNCTION}
        if [ $? -eq 0 ] ; then
            READY=1
        else
            sleep 5
        fi
        COUNT=$(expr ${COUNT} + 1)
    done
    if [ ${READY} -ne 1 ] ; then
        fail "${DESCRIPTION} not ready"
    fi
}

log() {
    local MSG="$1"
    local TS=$(date +%H:%M:%S)
    echo
    echo "${TS} containertest.sh: ${MSG}"
}

startcontainer() {
    local IMAGE=$1


    local DDCCORE_RPM=$(find ${ROOT_DIR}/ERICddc_CXP9030294/target -name 'ERICddccore_CXP9035927*.rpm')
    if [ -z "${DDCCORE_RPM}" ] ; then
        local RELEASE=$(wget -q -O - https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/repositories/releases/com/ericsson/oss/itpf/monitoring/ERICddccore_CXP9035927/maven-metadata.xml | grep release | sed 's/^ *<release>//' | sed 's|</release>.*||')
        DDCCORE_RPM="ERICddccore_CXP9035927-${RELEASE}.rpm"
        wget -O ${ROOT_DIR}/ERICddc_CXP9030294/target/${DDCCORE_RPM} "https://arm1s11-eiffel004.eiffel.gic.ericsson.se:8443/nexus/content/repositories/releases/com/ericsson/oss/itpf/monitoring/ERICddccore_CXP9035927/${RELEASE}/${DDCCORE_RPM}"
    fi

    docker run --rm \
        --volume ${ROOT_DIR}:${ROOT_DIR} \
        ${IMAGE} \
        bash ${ROOT_DIR}/ERICddc_CXP9030294/src/test/containertest.sh -a starttest

}

starttest() {
    local DDC_RPM=$(find ${ROOT_DIR}/ERICddc_CXP9030294/target/rpm/ERICddc_CXP9030294/ -name 'ERICddc_CXP9030294*.rpm')
    local DDCCORE_RPM=$(find ${ROOT_DIR}/ERICddc_CXP9030294/target -name 'ERICddccore_CXP9035927*.rpm')

    #
    # Get JBoss up and running
    #
    # Make service jboss monitor report JBoss started
    cat > /etc/init.d/jboss <<'EOF'
#!/bin/bash

grep --silent 'Admin console is not enabled' /var/tmp/jboss.log
EOF
    chmod 755 /etc/init.d/jboss

    cp ${ROOT_DIR}/e2e_test/target/e2e_test.war /opt/eap/standalone/deployments/

    # WA for
    # events [FIND_INITIAL_MBRS FIND_MBRS] are required by GMS, but not provided by any of the protocols below it
    sed -i 's|<protocol type="pbcast.GMS"/>||' /opt/eap/standalone/configuration/standalone-openshift.xml
    source /opt/eap/bin/launch/launch.sh
    $JBOSS_HOME/bin/standalone.sh -c standalone-openshift.xml -bmanagement 0.0.0.0 -Djboss.node.name=$(hostname) -Djboss.server.data.dir="$instanceDir" -Dwildfly.statistics-enabled=true > /var/tmp/jboss.log 2>&1 &

    # DDC looks for this directory to see if JBoss is present
    mkdir -p /ericsson/3pp
    ln -s /opt/eap /ericsson/3pp/jboss
    # DDC looks for this to figure out what version of EAP we have
    touch /ericsson/3pp/jboss/eap7.txt
    #
    # DDC expects to find java in /usr/java/default/bin/java
    #
    mkdir -p /usr/java
    ln -s  /usr/lib/jvm/java-1.8.0 /usr/java/default

    #
    #  Consul
    #
    mkdir -p /etc/consul.d/agent
    mkdir -p /tmp/consul/data
    cat > /etc/consul.d/agent/config.json <<EOF
{
  "retry_join" : ["127.0.0.1"],
  "data_dir": "/tmp/consul/data",
  "log_level": "INFO",
  "server": true,
  "node_name": "master",
  "addresses": {
    "https": "127.0.0.1"
  },
  "bind_addr": "127.0.0.1",
  "ui": false,
  "bootstrap_expect": 1
}
EOF
    /consul agent -dev -config-dir=/etc/consul.d/agent &

    #
    # Install ddc and ddc-core
    #
    mkdir -p /var/ericsson/ddc_data/

    rpm --install --nodeps ${DDCCORE_RPM} ${DDC_RPM}
    if [ $? -ne 0 ] ; then
        log "RPM Install failed"
        exit 1
    fi

    # DDC checks that /var/ericsson/ddc_data is in the mtab
    export DDC_DISABLE_NAS_CHECK=true

    # Check we can start
    /etc/init.d/ddc start
    if [ $? -ne 0 ] ; then
        log "ERROR: Start failed"
        exit 1
    fi

    #
    # Verify expected instr xml files are created
    #
    log "INFO: Wait for instr XML files"
    waitFor checkInstrXML "instr xml"

    #
    # Verify instr is started
    #
    export INSTR_IP=127.0.0.1
    log "INFO: Wait for instr ready"
    waitFor checkInstrStarted "instr"

    log "INFO: Verify metrics available"
    curl --max-time 30 --silent --show-error http://127.0.0.1:6789/metrics > /tmp/metrics.txt
    if [ $? -ne 0 ] ; then
        log "ERROR: Failed to request metrics"
        exit 1
    fi

    # Verify we have stats from genjmx_jboss.xml
    egrep --silent '^jboss_dh_lr_dh_jvm_dh_mem_dh_codecache_nm_Usage_dh_max{visibleby="INTERNAL",ns="genjmx_jboss",pn="jboss-lr-jmx"} [0-9]' /tmp/metrics.txt
    if [ $? -ne 0 ] ; then
        log "ERROR: Failed to find generic metric jboss_dh_lr_dh_jvm_dh_mem_dh_codecache_nm_Usage_dh_max"
        cat /tmp/metrics.txt
        exit 1
    fi

    # Verify we have stats threadpooljmx_jboss.xml
    egrep --silent '^jboss_dh_threadpool_dh_default_nm_completedTaskCount{visibleby="INTERNAL",ns="threadpooljmx_jboss",pn="jboss-jmx"} [0-9]' /tmp/metrics.txt
    if [ $? -ne 0 ] ; then
        log "ERROR: Failed to find threadpooljmx metric jboss_dh_threadpool_dh_default_nm_completedTaskCount"
        cat /tmp/metrics.txt
        exit 1
    fi

    # Verify we have stats from e2e
    egrep --silent '^jboss_dh_com_dt_ericsson_dt_cifwk_dt_diagmon_dt_util_dt_test_cl_type_eq_Test1_nm_attr1{visibleby="INTERNAL",ns="e2e_jboss",pn="jboss-Instrumentation"} [0-9]' /tmp/metrics.txt
    if [ $? -ne 0 ] ; then
        log "ERROR: Failed to find e2e metric Test1.attr1"
        cat /tmp/metrics.txt
        exit 1
    fi

    log "INFO: Verify e2e ping exits 0 indicating JBoss is up and running"
    /opt/ericsson/ERICddc/util/bin/e2eXMLGenerator --jmxurl "service:jmx:remote+http://127.0.0.1:9990" --jvmname primary --ping
    if [ $? -ne 0 ] ; then
        fail "ERROR: ping failed to exit with 0"
    fi

    log "INFO: Verify ddc stops"
    /etc/init.d/ddc stop
    if [ $? -ne 0 ] ; then
        log "ERROR: Start failed"
        exit 1
    fi

    log "INFO: Verify instr not running after stop"
    curl --max-time 30 --silent --show-error http://127.0.0.1:6789/metrics > /tmp/metrics.txt
    if [ $? -eq 0 ] ; then
        log "ERROR: instr still running"
        exit 1
    fi

    /bin/rm -rf /var/ericsson/ddc_data/*

    log "INFO: Verify SIDECAR task with JMX_SERVICE_URL"
    export SGNAME=testsg
    export INSTR_EXPORTER_PORT=6789
    export INSTR_IP=$(hostname -i)

    export JMX_SERVICE_URL="service:jmx:remote+http://127.0.0.1:9990"
    /opt/ericsson/ERICddc/monitor/monitorTasks SIDECAR > /var/ericsson/ddc_data/ddc.log 2>&1 &
    log "INFO: Wait for instr ready"
    waitFor checkInstrStarted "instr"
    log "INFO: Wait for instr XML files"
    waitFor checkInstrXML "instr xml"

    # Clean up
    log "INFO: Stop SIDECAR monitorTasks"
    pkill -f monitorTasks
    pkill -f s=instr
    /bin/rm -rf /var/tmp/ddc_data/*
    unset JMX_SERVICE_URL

    log "INFO: Verify SIDECAR task with JMX_SERVICE_URL"
    export DDC_JVM_LIST="jboss;service:jmx:remote+http://127.0.0.1:9990;yes;yes; jse;service:jmx:rmi:///jndi/rmi://127.0.0.1:9999/jmxrmi;yes;no;"
    /opt/ericsson/ERICddc/monitor/monitorTasks SIDECAR  > /var/ericsson/ddc_data/ddc.log 2>&1 &
    log "INFO: Check that ddc is waiting for the jse JVM target"
    LOG_CONTAINS="jse not ready"
    waitFor checkLogContains "${LOG_CONTAINS}"

    log "INFO: Check that ddc continues once j2e target is available"
    /usr/java/default/bin/java \
      -cp ${ROOT_DIR}/e2e_test/target/classes \
      -Dcom.sun.management.jmxremote.port=9999 \
      -Dcom.sun.management.jmxremote.authenticate=false \
      -Dcom.sun.management.jmxremote.ssl=false \
      com.ericsson.cifwk.diagmon.util.e2e_test.Test1 &
    LOG_CONTAINS="jse ready"
    waitFor checkLogContains "${LOG_CONTAINS}"
    log "INFO: Wait for instr JBoss XML files"
    waitFor checkInstrXML "instr jboss xml"
    log "INFO: Wait for jse instr XML files"
    waitFor checkJseInstrXML "instr jse xml"

    log "INFO: All tests pass"
    exit 0
}

while getopts  "a:i:" flag ; do
    case "$flag" in
        a) ACTION="$OPTARG";;
        i) IMAGE="$OPTARG";;
    esac
done

if [ "${ACTION}" = "startcontainer" ] ; then
    startcontainer ${IMAGE}
elif [ "${ACTION}" = "starttest" ] ; then
    starttest
fi
