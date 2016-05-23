/**
*  Copyright 2016 Philip Rosenberg-Watt
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/
metadata {
    definition (name: "Thermostat Fan Controller", namespace: "PhilRW", author: "PhilRW") {
        capability "Switch"
    }

    //simulator metadata
    simulator {
        // status messages
        status "on": "switch:on"
        status "off": "switch:off"

        // reply messages
        reply "on": "switch:on"
        reply "off": "switch:off"
    }

    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, chanChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821"
            state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff"
        }

        main "switch"
        details "switch"
    }
}

// parse events into attributes
def parse(description) {
    log.debug "Parsing '${description}'"

    def pair = description.split(":")
    createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def on() {
    log.trace "on()"
    log.debug "parent: ${parent}"

    parent.start("controller")
}

def off() {
    log.trace "off()"
    log.debug "parent: ${parent}"

    parent.stop("controller")
}