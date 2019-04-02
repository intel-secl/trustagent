from subprocess import call, check_output
import json
import requests
import jprops
import argparse
import psutil
import time
import os
import uuid
import concurrent.futures
from multiprocessing.pool import ThreadPool as Pool

path = '/opt/trustagent-simulator'
#path = os.environ.get('TA_SIMULATOR_HOME')

def start_mock_TA(port):
    call(["flask","run", "--host=0.0.0.0", "--port="+str(port), "--cert=../configuration/cert.pem", "--key=../configuration/key.pem"]) 


def connection_string_create(port):
    connection = "https://" + properties['tagent.simulator.ip'] + ":" + str(port) + ";u=" +properties['tagent.simulator.username'] + ";p=" + properties['tagent.simulator.password']
    return connection


def stop_mock_TA():
    pid = ""
    port = int(properties['tagent.simulator.port.start'])
    while(port <= int(properties['tagent.simulator.port.stop'])):
        output = check_output(["ss", "-lptn", "sport = :"+str(port)])
        if "pid" in output:
            position = output.find("pid")
            flag = 0 
	    for pos, char in enumerate(output):
                if flag == 1:
                    break
                if pos > (position + 3):
                    if char == ',':
                        flag = 1
                    else:
                        pid = pid + char
            call(["sudo", "kill", "-9", str(pid)])        
            pid = ""        
        port = port + 1
    print("[INFO] "+str(int(properties['tagent.simulator.port.stop']) - int(properties['tagent.simulator.port.start']) + 1)+" Mock Trust Agent Servers stopped............")

def post_create_host_request(json_data):
    mtwilson_url = properties['mtwilson.api.url'] + 'v2/hosts'
    resp = requests.post(mtwilson_url, json = json_data, auth=(mtwilson_user, mtwilson_password),verify=False)
    return resp.content

def create_hosts():
    start_time = time.time()

    mtwilson_url = properties['mtwilson.api.url'] + 'v2/hosts'
    port = int(properties['tagent.simulator.port.start'])
    host_name = properties['tagent.simulator.ip']

    connection_string = connection_string_create(port)
    json_data = {"host_name": host_name,"connection_string": connection_string}
    resp = requests.post(mtwilson_url, json = json_data, auth=(mtwilson_user, mtwilson_password),verify=False)
    log.write(resp.content);
    port = port + 1

    json_data_dump = []
    while port <= int(properties['tagent.simulator.port.stop']):
        host_name = 'Purley14_' + str(uuid.uuid4()) + '_' + str(port)
        connection_string = connection_string_create(port)
        json_data_dump.append({"host_name": host_name,"connection_string": connection_string})
        port = port + 1
    log.write("Spawning %s threads" % number_of_threads)
    start=0
    while(start <= len(json_data_dump)):
        end = start + request_volume
        temp_list = list(json_data_dump[slice(start, end)])
        start = end
        time.sleep(request_delay)
        with concurrent.futures.ThreadPoolExecutor(max_workers=number_of_threads) as executor:
            futures = executor.map(post_create_host_request, [
                json_data
                for json_data in temp_list
            ])
            for response in futures:
                log.write(response)
                pass
    log.write("\n-------------------------------------------------------------------------------------------\n")
    print("Time taken for host registration is : %s seconds" % (time.time() - start_time))
    log.write("\n-------------------------------------------------------------------------------------------\n")

def post_create_flavor_request(json_data):
    mtwilson_url = properties['mtwilson.api.url'] + 'v2/flavors'
    resp = requests.post(mtwilson_url, json = json_data, auth=(mtwilson_user, mtwilson_password),verify=False)
    return resp.content

def create_flavors():
    start_time = time.time()

    port = int(properties['tagent.simulator.port.start'])
    number_of_distinct_flavors = properties['tagent.simulator.flavor.iterator']
    partial_flavor_list = ["BIOS", "OS"]
    partial_unique_flavor_list = ["ASSET_TAG", "HOST_UNIQUE"] #, "COMBINED"]
    no_of_trusted_host = (int(properties['tagent.simulator.port.stop']) - int(properties['tagent.simulator.port.start']) + 1) * int(properties['tagent.simulator.trusted.hosts']) / 100
    json_data_dump = []
    while port < (int(properties['tagent.simulator.port.start']) + no_of_trusted_host):
        connection_string = connection_string_create(port)
        json_data_dump.append({"connection_string": connection_string, "partial_flavor_types": partial_unique_flavor_list})
        port = port + 1

    port = int(properties['tagent.simulator.port.start'])
    print((int(properties['tagent.simulator.port.start']) + int(number_of_distinct_flavors)))
    while port < (int(properties['tagent.simulator.port.start']) + int(number_of_distinct_flavors)) and port < (int(properties['tagent.simulator.port.start']) + no_of_trusted_host):
        connection_string = connection_string_create(port)
        json_data_dump.append({"connection_string": connection_string, "partial_flavor_types": partial_flavor_list})
        port = port + 1

    start=0
    while(start <= len(json_data_dump)):
        end = start + request_volume
        temp_list = list(json_data_dump[slice(start, end)])
        start = end
        time.sleep(request_delay)
        with concurrent.futures.ThreadPoolExecutor(max_workers=number_of_threads) as executor:
            futures = executor.map(post_create_flavor_request, [
                json_data
                for json_data in temp_list
            ])
            for response in futures:
                log.write(response)
                pass
    log.write("\n-------------------------------------------------------------------------------------------\n")
    print("Time taken for flavor creation is : %s seconds" % (time.time() - start_time))
    log.write("\n-------------------------------------------------------------------------------------------\n")

"""def get_stats():
    total_memory_usage_GB = 0.0
    total_CPU_usage = 0.0
    pid = ""
    port = int(properties['tagent.simulator.port.start'])
    while(port <= int(properties['tagent.simulator.port.stop'])):
        output = check_output(["ss", "-lptn", "sport = :"+str(port)])
        if "pid" in output:
            position = output.find("pid")
            flag = 0
            for pos, char in enumerate(output):
                if flag == 1:
                    break
                if pos > (position + 3):
                    if char == ',':
                        flag = 1
                    else:
                        pid = pid + char

            stats_of = psutil.Process(int(pid))
            #with stats_of.oneshot():
            total_CPU_usage = total_CPU_usage + stats_of.cpu_percent(interval = 1)
            total_memory_usage_GB = stats_of.memory_percent()
            pid = ""
        port = port + 1
    print(total_CPU_usage)
    print(total_memory_usage_GB)"""

if __name__ == "__main__":
    global properties, log, mtwilson_user, mtwilson_password, number_of_threads, request_volume, request_delay
    file_read = open(path+"/configuration/trustagent-simulator.properties", "r")
    log = open(path+"/logs/tagent-sim.log", "a")
    properties = jprops.load_properties(file_read)
    file_read.close()
    mtwilson_user     = properties['mtwilson.api.username']
    mtwilson_password = properties['mtwilson.api.password']
    number_of_threads = properties['tagent.simulator.request.threads']
    request_volume = int(properties['tagent.simulator.request.volume'])
    request_delay = int(properties['tagent.simulator.request.volume.delay'])

    parser = argparse.ArgumentParser(description='Start or Stop mock TA server')
    parser.add_argument("--start", action="store_true", help="Start the mock TA server(s)")
    parser.add_argument("--stop", action="store_true", help="Stop the mock TA server(s)")
    parser.add_argument("--create_all_hosts", action="store_true", help="Create hosts")
    parser.add_argument("--create_all_flavors", action="store_true", help="Create flavors for the hosts")
    #parser.add_argument("--get_stats", action="store_true", help="Get performance stats")
    args = parser.parse_args()
    if args.stop:
        stop_mock_TA()
    elif args.start:
        port = int(properties['tagent.simulator.port.start'])
        pool = Pool(int(properties['tagent.simulator.port.stop']) - int(properties['tagent.simulator.port.start']) + 1)
        while(port <= int(properties['tagent.simulator.port.stop'])):
            pool.apply_async(start_mock_TA, (port,))
            port = port + 1
        pool.close()
        pool.join()
    elif args.create_all_hosts:
        create_hosts()
    elif args.create_all_flavors:
        create_flavors()
    """elif args.get_stats:
        get_stats()"""
