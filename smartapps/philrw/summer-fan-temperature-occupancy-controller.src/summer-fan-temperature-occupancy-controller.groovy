/**
*  Summer Fan Temperature + Occupancy Controller
*
*  Copyright 2016 Philip Rosenberg-Watt
*
*	Version History
*
*	1.2		2016-05-21		Add fan state tracking
*	1.1		2016-05-04		Optionally run automatically only when room is occupied
*	1.0		2016-04-23		Initial version
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
    name: "Summer Fan Temperature + Occupancy Controller",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Sets fan speed based on temperature and (optionally) motion",
    category: "My Apps",
    iconUrl:   "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png")


preferences {
    page(name: "prefPage")
}

def prefPage() {
    dynamicPage(name: "prefPage", title: "Fan Temperature + Occupancy Controller", install: true, uninstall: true) {
        section("Devices") {
            input "theThermostat", "capability.temperatureMeasurement", title: "Thermostat:", multiple: false, required: true
            input "switches", "capability.switchLevel", title: "Fan(s) to control:", multiple: true, required: true
        }
        section("Settings") {
            input "lowThreshold", "decimal", title: "On low if at or above:", defaultValue: 23, required: false
            input "mediumThreshold", "decimal", title: "On medium if at or above:", defaultValue: 25, required: false
            input "highThreshold", "decimal", title: "On high if at or above:", defaultValue: 27, required: false
            input "runOnOccupied", "bool", title: "Only run when room is occupied?", defaultValue: false, submitOnChange: true
            if (runOnOccupied) {
                input "theMotionSensor", "capability.motionSensor", title: "Motion sensor:", multiple: false, required: true
                input "motionSensorTimeout", "number", title: "Off after this many minutes:", required: true
            }
        }
        section([title: "App Instance", mobileOnly: true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)"
        }

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
    subscribe(theThermostat, "temperature", tempChangeHandler)
    if (runOnOccupied) {
        subscribe(theMotionSensor, "motion.active", motionActiveHandler)
        subscribe(theMotionSensor, "motion.inactive", motionInactiveHandler)
    }
}


def tempChangeHandler(evnt) {
    log.trace "tempChangeHandler(${evnt}) - temperature update, state: ${state}"
    def temp = evnt.doubleValue

    if (runOnOccupied) {
        checkMotion()
    } else {
        checkTemp(temp)
    }
}


def motionActiveHandler(evnt) {
    if (runOnOccupied) {
        log.trace "motionActiveHandler(${evnt}) - motion active, state: ${state}"
        def temp = theThermostat.currentState("temperature")

        checkTemp(temp.doubleValue)
    }
}


def motionInactiveHandler(evnt) {
    if (runOnOccupied) {
        log.trace "motionInactiveHandler(${evnt}) - motion inactive, state: ${state}"

        runIn(60 * motionSensorTimeout, checkMotion)
    }
}


def checkTemp(temp) {
    log.trace "checkTemp(${temp}), state: ${state}"

    log.debug "Temp is ${temp}. Low/Med/Hi = ${lowThreshold}/${mediumThreshold}/${highThreshold}"
    if (state.level != "high" && temp >= highThreshold) {
        log.debug "Setting level to high, 99"
        switches.setLevel(99);
        state.level = "high"
    } else if (state.level != "medium" && temp >= mediumThreshold) {
        log.debug "Setting level to medium, 67"
        switches.setLevel(67);
        state.level = "medium"
    } else if (state.level != "low" && temp >= lowThreshold) {
        log.debug "Setting level to low, 33"
        switches.setLevel(33);
        state.level = "low"
    } else if (state.level != "off") {
        log.debug "Temperature not within range, setting level to off, 0"
        switches.setLevel(0)
        state.level = "off"
    } else {
        log.debug "Everything is set up correctly, no change needed."
    }
}


def checkMotion() {
    log.trace "checkMotion(), state: ${state}"

    def motionState = theMotionSensor.currentState("motion")
    def elapsed = now() - motionState.date.time
    def threshold = 1000 * ( ( 60 * motionSensorTimeout ) - 1 )

    if (state.level != "off" && motionState.value == "inactive" && elapsed >= threshold) {
        log.debug "Motion has stayed inactive long enough since last check ($elapsed ms): turn fan(s) off"

        switches.setLevel(0)
        state.level = "off"
    } else {
        log.debug "Motion is active or not inactive long enough, check temperature"
        def temp = theThermostat.currentState("temperature")

        checkTemp(temp.doubleValue)
    }
}
