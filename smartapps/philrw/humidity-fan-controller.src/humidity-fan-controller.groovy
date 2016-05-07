/**
*  Humidity Fan Controller
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
    name: "Humidity Fan Controller",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Turn on exhaust fan when humidity rises above a certain level, then turn off when it reaches a different level.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Bath/bath6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Bath/bath6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Bath/bath6-icn@2x.png")


preferences {
    page(name: "prefPage")
}

def prefPage() {
    dynamicPage(name: "prefPage", title: "Humidity Fan Controller", install: true, uninstall: true) {
        section("Devices") {            
            input "theSwitch", "capability.switch", title: "Exhaust fan"
            input "rhSensor", "capability.relativeHumidityMeasurement", title: "Humidity sensor"
        }
        section("Settings") {
            input "rhMax", "number", title: "Turn on fan above this level (RH%)", defaultValue: 60
            input "rhTarget", "number", title: "Turn off fan below this level (RH%)", defaultValue: 50
            input "runOnUnoccupied", "bool", title: "Only run when room is unoccupied?", defaultValue: false, submitOnChange: true
            if (runOnUnoccupied) {
                input "motionSensor", "capability.motionSensor", title: "Which motion sensor?"
                input "motionSensorTimeout", "number", title: "After how many seconds of inactivity?"
                input "useThermostatFan", "bool", title: "Use thermostat fan?", defaultValue: false, submitOnChange: true
                if (useThermostatFan) {
                    input "thermostatFan", "capability.thermostatFanMode", title: "Thermostat fan"
                }
            }
        }
        section([title: "App Instance", mobileOnly: true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)"
        }
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
    log.trace "initialize()"

    state.runFan = false
    state.runThermostatFan = false

    subscribe(rhSensor, "humidity", rhHandler)
    if (runOnUnoccupied) {
        subscribe(motionSensor, "motion.active", motionActiveHandler)
        subscribe(motionSensor, "motion.inactive", motionInactiveHandler)
    }
}

def motionActiveHandler(evnt) {
    if (runOnUnoccupied) {
        log.trace "motionActiveHandler(${evnt})"

        if (state.runFan) {
            log.debug "Fan is running, turning off switch..."

            theSwitch.off()
        }
    }
}

def motionInactiveHandler(evnt) {
    if (runOnUnoccupied) {
        log.trace "motionInactiveHandler(${evnt})"
        log.debug "Wait ${motionSensorTimeout} seconds for motion to stop..."

        runIn(motionSensorTimeout, checkMotion)
    }
}

def rhHandler(evnt) {
    log.trace "rhHandler(${evnt})"
    def rh = evnt.value.toInteger()

    if (runOnUnoccupied) {
        checkMotion()
        if (useThermostatFan) {
            thermostatFanController(rh)
        }
    } else {
        fanController(rh)
    }
}

def checkMotion() {
    log.trace "checkMotion()"

    def motionState = motionSensor.currentState("motion")

    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.date.time
        def threshold = 1000 * ( motionSensorTimeout - 1 )

        if (elapsed >= threshold) {
            def rh = rhSensor.currentValue("humidity")

            log.debug "Motion has stayed inactive long enough since last check (${elapsed} ms): fanController(${rh})"
            fanController(rh)
        } else {
            log.debug "Motion has not stayed inactive long enough since last check (${elapsed} ms): do nothing"
        }
    } else {
        log.debug "Motion is active: do nothing."
    }
}


def fanController(rh) {
    log.trace "fanController(${rh})"

    log.debug "Current RH is ${rh}%, max is ${rhMax}%, target is ${rhTarget}%."
    if (state.runFan) {
        if (rh < rhTarget) {
            log.debug "Turning off switch..."
            theSwitch.off()
            state.runFan = false
        } else {
            log.debug "Reactivating switch..."
            theSwitch.on()
        }
    } else if (!state.runFan && rh > rhMax) {
        log.debug "Turning on switch..."
        theSwitch.on()
        state.runFan = true
    } else {
        log.debug "RH not in actionable range: do nothing."
    }   
}


def thermostatFanController(rh) {
    log.trace "thermostatFanController(${rh})"

    log.debug "Current RH is ${rh}%, max is ${rhMax}%, target is ${rhTarget}%."
    if (state.runThermostatFan && rh < rhTarget) {
        log.debug "Resetting thermostat fan..."
        thermostatFan.setThermostatFanMode(state.previousFanMode)
        state.runThermostatFan = false
    } else if (!state.runThermostatFan && rh > rhMax) {
        log.debug "Turning on thermostat fan..."
        state.previousFanMode = thermostatFan.currentValue("thermostatFanMode")
        log.trace "Thermostat fan was set to ${state.previousFanMode}."
        thermostatFan.setThermostatFanMode("on")
        state.runThermostatFan = true
    } else {
        log.debug "RH not in actionable range: do nothing."
    }   
}