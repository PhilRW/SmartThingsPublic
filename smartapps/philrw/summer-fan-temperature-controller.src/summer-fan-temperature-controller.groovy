/**
 *  Summer Fan Temperature Controller
 *
 *  Copyright 2016 Philip Rosenberg-Watt
 *
 *	Version History
 *
 *	1.0.0	2016-04-23		Initial version
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
definition(
    name: "Summer Fan Temperature Controller",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Controls fan speed based on temperature",
    category: "My Apps",
    iconUrl:   "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png")


preferences {
    section("Set fan controls:") {
        input "theThermostat", "capability.temperatureMeasurement", title: "Thermostat:", multiple: false, required: true
        input "switches", "capability.switchLevel", title: "Fan(s) to control:", multiple: true, required: true
       	input "lowThreshold", "decimal", title: "On low if at or above:", defaultValue: 22.5, required: false
       	input "mediumThreshold", "decimal", title: "On medium if at or above:", defaultValue: 25, required: false
       	input "highThreshold", "decimal", title: "On high if at or above:", defaultValue: 27.5, required: false
   	}
}


def installed() {
	log.info "Summer Fan Occupancy Controller: ${settings}"
	initialize()
}

def updated() {
	log.trace "Summer Fan Occupancy Controller - updated()"
    unsubscribe()
    initialize()
}

def initialize() {
	log.trace "Summer Fan Occupancy Controller - initialize()"
    subscribe(theThermostat, "temperature", checkTemp)
}
 
def checkTemp(evnt) {    
    def temp = evnt.doubleValue
    log.debug "Temp is ${temp}. Low/Med/Hi = ${lowThreshold}/${mediumThreshold}/${highThreshold}"
    if (temp >= highThreshold) {
        log.debug "Setting level to high, 99"
        switches.setLevel(99);
    } else if (temp >= mediumThreshold) {
        log.debug "Setting level to medium, 67"
        switches.setLevel(67);
    } else if (temp >= lowThreshold) {
        log.debug "Setting level to low, 33"
        switches.setLevel(33);
    } else {
        log.debug "Temperature not within range, setting level to off, 0"
        switches.setLevel(0)
    }
}