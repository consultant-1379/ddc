#!/bin/bash

OUTPUT_FILE=$1
REPO_URL=$2
GROUP_ID=$3
ARTIFACT_ID=$4
VERSION=$5

if [[ "${VERSION}" =~ "SNAPSHOT" ]] ; then
    echo "Not writing snapshot version ${RPM_VERSION}"
else
    echo "Writing RPM_VERSION '${VERSION}' to ${OUTPUT_FILE}"
    GROUP_PATH=$(echo ${GROUP_ID} | sed 's|\.|/|g')
    cat > $OUTPUT_FILE <<EOF
RPM_VERSION=${VERSION}
RPM_URL=${REPO_URL}/${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}.rpm
EOF
fi


