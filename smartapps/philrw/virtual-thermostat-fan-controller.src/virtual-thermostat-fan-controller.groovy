/**
*  Virtual Thermostat Fan Controller
*
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
definition(
    name: "Virtual Thermostat Fan Controller",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Control the thermostat fan with a switch.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn@2x.png")


preferences {
    section("Title") {
        input "theSwitch", "capability.switch", title: "The switch"
        input "theThermostatFan", "capability.thermostatFanMode", title: "The thermostat"
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
    state.theThermostatFanMode = theThermostatFan.currentValue("thermostatFanMode")
    state.running = false
    subscribe(theSwitch, "switch", switchHandler)
}


def switchHandler(evnt) {
    log.trace "switchHandler($evnt)"
    if (evnt.value == "off") {
        turnOff()
    } else if (evnt.value == "on") {
        turnOn()
    }
}


def turnOn() {
    log.trace "turnOn()"

    if (!state.running) {
        log.debug "Turning on thermostat fan..."
        state.theThermostatFanMode = theThermostatFan.currentValue("thermostatFanMode")
        theThermostatFan.setThermostatFanMode("on")
        state.running = true
    } else {
        log.debug "Thermostat fan already running. Command ignored."
    }
}


def turnOff() {
    log.trace "turnOff()"

    if (state.running) {
        log.debug "Resetting thermostat fan to ${state.theThermostatFanMode}..."
        theThermostatFan.setThermostatFanMode(state.theThermostatFanMode)
        state.running = false
    } else {
        log.debug "Thermostat fan mode currently not overridden by this app. Command ignored."
    }
}