/**
 *  Nest Auto Away
 *
 *  Copyright 2015 Philip Rosenberg-Watt
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
    name: "Nest Auto Away",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Set Nest Thermostat away status based on location mode",
    category: "My Apps",
    iconUrl: "https://dl.dropboxusercontent.com/u/2421186/nestaway.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2421186/nestaway%402x.png")

preferences {
	section("Select away mode:") {
		input "modes", "mode", multiple: true
    }
    section("Control these thermostats:") {
        input "thermostats", "capability.thermostat", required: true, multiple: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler($evnt) {
	log.debug("location.currentMode = ${location.currentMode}")
    log.debug("selected away modes = $modes")
    if (modes.contains(location.currentMode)) {
    	thermostats.each { it.away() }
    } else {
        thermostats.each { it.present() }
    }
}