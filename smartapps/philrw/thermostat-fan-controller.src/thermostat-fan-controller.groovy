/**
*  Thermostat Fan Controller
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
    name: "Thermostat Fan Controller",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Control the thermostat fan with a virtual switch.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home30-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home30-icn@2x.png")


preferences {
    section("Title") {
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

    def controller = getController()
    if (controller) {
        controller.label = app.label
    }

    initialize()
}


def initialize() {
    state.theThermostatFanMode = theThermostatFan.currentValue("thermostatFanMode")
    state.running = false

    if (!getAllChildDevices()) {
        // create controller device and set name to the label used here
        def dni = "${new Date().getTime()}"
        log.debug "app.label: ${app.label}"
        addChildDevice("PhilRW", "Thermostat Fan Controller", dni, null, ["label": app.label])
        state.controllerDni = dni
    }

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


public def start(source) {
    log.trace "start(${source})"

    sendStartEvent(source)
    turnOn()
}


public def stop(source) {
    log.trace "stop(${source})"

    sendStopEvent(source)
    turnOff()
}


def sendControllerEvent(eventData) {
    def controller = getController()
    if (controller) {
        controller.controllerEvent(eventData)
    }
}


def getController() {
    def dni = state.controllerDni
    if (!dni) {
        log.warn "no controller dni"
        return null
    }
    def controller = getChildDevice(dni)
    if (!controller) {
        log.warn "no controller"
        return null
    }
    log.debug "controller: ${controller}"
    return controller
}


def sendStartEvent(source) {
    log.trace "sendStartEvent(${source})"

    def eventData = [
        name: "switch",
        value: "on",
        descriptionText: "${app.label} has turned on",
        displayed: true,
        linkText: app.label,
        isStateChange: true
    ]

    sendControllerEvent(eventData)
}


def sendStopEvent(source) {
    log.trace "sendStopEvent(${source})"

    def eventData = [
        name: "switch",
        value: "off",
        descriptionText: "${app.label} has turned off",
        displayed: true,
        linkText: app.label,
        isStateChange: true
    ]

    sendControllerEvent(eventData)
}