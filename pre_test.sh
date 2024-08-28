#!/bin/bash

fileList='ERICddc_CXP9030294/src/main/resources/etc/filesERICddcCollects.txt'
mdFile='ERICddc_CXP9030294/src/main/resources/etc/ERICddcPrivacyInfo.md'

echo "Running parsePrivacyInfo"
grep -hr '#FILE_INFO' ERICddc_CXP9030294/src/main/resources/* | perl parsePrivacyInfo --fileList ${fileList} --md ${mdFile}

if [ $? -ne 0 ] ; then
  echo "ERROR: parsePrivacyInfo failed"
  echo "Exiting: pre_test.sh"
  exit 1
fi
