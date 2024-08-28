#!/bin/bash
"true" '''\'
P=$(which python3 2>/dev/null) || P=$(which python); exec $P "$0" "$@"
'''
'''
Script to extract vm performance data
'''

import ssl
import atexit
from pyVmomi import vim
from pyVim.connect import SmartConnect, Disconnect
import time
import datetime
from pyVmomi import vmodl
from threading import Thread
import argparse
import socket
import os

DDC_PATH="/opt/ericsson/ERICddc"

def read_metrics():
    perfcounter = [line.strip() for line in open(DDC_PATH+"/util/bin/esxi_metrics.config", 'r')]
    return perfcounter

def get_esxi_metrics(content,esxi_host,esxiMetric,outputDir,startDateTime,endDateTime):
    try:
        metricNames = {}
        metric_unitInfo = {}
        perfManager = content.perfManager
        perfList = content.perfManager.perfCounter
        for counter in perfList: #build the vcenter counters for the objects
            counter_full = "{0}.{1}.{2}".format(counter.groupInfo.key,counter.nameInfo.key,counter.rollupType)
            metricNames[counter_full] = counter.key
            metric_unitInfo[counter_full] = counter.unitInfo.key
        counterId = metricNames[esxiMetric]

        startTime =  datetime.datetime.strptime(startDateTime, '%Y-%m-%d %H:%M:%S')
        endTime = datetime.datetime.strptime(endDateTime, '%Y-%m-%d %H:%M:%S')

        metricId = vim.PerformanceManager.MetricId(counterId=counterId, instance="")
        search_index = content.searchIndex
        host = search_index.FindByDnsName(dnsName=esxi_host, vmSearch=False)
        query = vim.PerformanceManager.QuerySpec(maxSample=1,entity=host,metricId=[metricId],startTime=startTime,endTime=endTime)
        stats=perfManager.QueryPerf(querySpec=[query])

        count=0
        output=[]
        f = open(outputDir+"/esxi_metrics.txt",'a')
        for val in stats[0].value[0].value:
            perfinfo={}
            time=datetime.datetime.strftime(stats[0].sampleInfo[count].timestamp, '%Y-%m-%d %H:%M:%S')
            perfinfo['timestamp']=time
            perfinfo['value']=val
            output.append(perfinfo)
            count+=1
        for out in output:
            f.write("{0};{1}({2});{3}\n".format (out['timestamp'],esxiMetric,metric_unitInfo[esxiMetric],out['value']))
        f.close()
    except Exception as e:
        print("Caught exception : " + str(e) + "\t" + esxi_host + "\t" + esxiMetric)

def get_host_info(outputDir,host):
    f = open(outputDir+"/dmidecode.txt",'w')
    summary = host.summary
    f.write("Handle ESXI,\n{0}".format( "System Information"))
    config = summary.config
    f.write( "\n\t{0} {1}".format("IPAddress:", config.name))
    f.write( "\n\t{0} {1}".format("vim.host.Summary:",summary.host))
    hardware = summary.hardware
    f.write( "\n\t{0} {1}".format("Manufacturer:", hardware.vendor))
    f.write( "\n\t{0} {1}".format("Product Name:", hardware.model))
    f.write("\n\nHandle ESXI,\n{0}".format( "Processor Information"))
    f.write( "\n\t{0} {1}".format("Version:", hardware.cpuModel))
    f.write( "\n\t{0} {1}".format("NumCpuPkgs:", hardware.numCpuPkgs))
    f.write( "\n\t{0} {1}".format("Core Count:", hardware.numCpuCores))
    f.write( "\n\t{0} {1}".format("Thread Count:", hardware.numCpuThreads))
    f.write( "\n\t{0} {1}".format("NumNics:", hardware.numNics))
    f.write("\n\nHandle ESXI,\n{0}".format( "Memory Device"))
    f.write( "\n\t{0} {1} MB".format("Size:", hardware.memorySize/1024/1024))
    runtime = summary.runtime
    f.write("\n\nHandle ESXI,\n{0}".format( "BIOS Information"))
    f.write( "\n\t{0} {1}\n".format("Release Date:", datetime.datetime.strftime(runtime.bootTime, '%m/%d/%Y')))

def GetArgs():
   """
   Supports the command-line arguments listed below.
   """
   parser = argparse.ArgumentParser(
       description='Process args for retrieving all the Virtual Machines')
   parser.add_argument('-s', '--host', required=True, action='store',
                       help='Remote host to connect to')
   parser.add_argument('-o', '--port', type=int, default=443, action='store',
                       help='Port to connect on')
   parser.add_argument('-u', '--user', required=True, action='store',
                       help='User name to use when connecting to host')
   parser.add_argument('-p', '--password', required=False, action='store',
                       help='Password to use when connecting to host')
   parser.add_argument('-d', '--directory', required=False, action='store',
                       help='outputpath directory')
   parser.add_argument('-st', '--startDateTime', required=False, action='store',
                       help='Start Time')
   parser.add_argument('-et', '--endDateTime', required=False, action='store',
                       help='End Time')
   parser.add_argument('-dt', '--date', required=False, action='store',
                       help='Date')

   args = parser.parse_args()
   return args

def main():
   args = GetArgs()

   sslContext = ssl.create_default_context()
   sslContext.check_hostname = False
   sslContext.verify_mode = ssl.CERT_NONE

   try:
        si = SmartConnect(
            host=args.host,
            user=args.user,
            pwd=args.password,
            port=args.port,
            sslContext=sslContext
           )
        atexit.register(Disconnect, si)

   except Exception as e:
        raise SystemExit("\nUnable to connect to the provided ", args.host," Please check credentials..\n")

   if not si:
        raise SystemExit("Unable to connect to host ", args.host)

   content = si.RetrieveContent()
   objview = content.viewManager.CreateContainerView(content.rootFolder,
                                                          [vim.HostSystem],
                                                          True)
   esxi_hosts = objview.view

   for esx in esxi_hosts:
          summary=esx.summary
          esxi_host=summary.config.name
          (hostname, _, ip_address_list) = socket.gethostbyaddr(esxi_host)

          outputDir=args.directory+"/"+hostname.split('.')[0]+"_ESXI/"+args.date+"/server"
          try:
             os.makedirs(outputDir)
          except OSError:
             if not os.path.isdir(outputDir):
                 raise

          if not os.path.exists(outputDir+"/hostname") or os.stat(outputDir+"/hostname").st_size == 0:
             with open(outputDir+"/hostname", 'w') as f:
                 f.write("{0} \t {1}".format(esxi_host,hostname.split('.')[0]))
                 f.close()
          if not os.path.exists(outputDir+"/dmidecode.txt") or os.stat(outputDir+"/dmidecode.txt").st_size == 0:
                 get_host_info(outputDir,esx)
   
          esxi_metrics=read_metrics()
  
          for esxiMetric in esxi_metrics:
              get_esxi_metrics(content,esxi_host,esxiMetric,outputDir,args.startDateTime,args.endDateTime)

if __name__ == "__main__":
    main()
