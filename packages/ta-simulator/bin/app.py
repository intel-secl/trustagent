from flask import Flask, request, make_response, jsonify
from flask_basicauth import BasicAuth
import jprops
import json
import uuid
import time
import os

app = Flask(__name__)
path = '/opt/trustagent-simulator'
file_read = open(path+"/configuration/trustagent-simulator.properties")
properties = jprops.load_properties(file_read)
#path = os.environ.get('TA_SIMULATOR_HOME')
app.config['BASIC_AUTH_USERNAME'] = properties['tagent.simulator.username']
app.config['BASIC_AUTH_PASSWORD'] = properties['tagent.simulator.password']
app.config['BASIC_AUTH_FORCE'] = True

@app.before_request
def before_request():
    if True:
        print("HEADERS", request.headers)
        print("REQ_path", request.path)
        print("ARGS",request.args)
        print("DATA",request.data)
        print("FORM",request.form)

@app.route("/v2/aik", methods =['GET'])
def aik():
       f = open(path+"/repository/aik.cert","r")
       resp = make_response(f.read(), 200)
       f.close()
       resp.headers['Content-Type'] = 'application/pkix-cert'
       return resp


@app.route("/v2/tpm/quote", methods = ['POST'])
def quote():
    sleep_time = int(properties['tagent.simulator.api.response.delay'])
    if request.method == 'POST' and request.headers.get('Accept') == 'application/xml':
       f = open(path+"/repository/quote.xml","r")
       resp = make_response(f.read(), 200)
       f.close()
       resp.headers['Content-Type'] = 'application/xml'
       time.sleep(sleep_time)
       return resp

@app.route("/v2/host", methods = ['GET'])
def host_info():
    url = request.url_root
    port = url.rpartition(':')[2]
    port = port[:-1] 
    file_read_host_info = open(path+"/repository/host_info.json", "r")
    host_info = json.load(file_read_host_info)
    number_of_distinct_flavors = properties['tagent.simulator.flavor.iterator']
    host_info['bios_name'] = host_info['bios_name'] + "-" + str(int(port) % int(number_of_distinct_flavors))
    host_info['bios_version'] = host_info['bios_version'] + "-" + str(int(port) % int(number_of_distinct_flavors))
    host_info['os_version'] = host_info['os_version'] + "." + str(int(port) % int(number_of_distinct_flavors))
    host_info['vmm_name'] = host_info['vmm_name'] + "-" + str(int(port) % int(number_of_distinct_flavors))
    host_info['vmm_version'] = host_info['vmm_version'] + "." + str(int(port) % int(number_of_distinct_flavors))
    host_info['hardware_uuid'] = uuid.uuid4()
    #host_info['hardware_uuid'] = '2bc5e52a-5b31-41af-865d-da8e41a4e44d'
    host_info['host_name'] = host_info['host_name'] + str(port)
    print(host_info)
    return jsonify(host_info)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port='5000')
