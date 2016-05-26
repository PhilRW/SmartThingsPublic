/**
*  Switch with Motion and Presence
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
    name: "Switch with Motion and Presence",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Control switch with motion and presence",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light23-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light23-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light23-icn@2x.png")


preferences {
    section("Configuration") {
        input "thePresenceSensor", "capability.presenceSensor", title: "When [this] is present"
        input "theMotionSensor", "capability.motionSensor", title: "...and [this] detects motion..."
        input "theSwitch", "capability.switch", title: "...turn on [this]..."
        input "motionSensorTimeout", "number", title: "...and turn off after [this many] minutes of no motion..."
        input "startTime", "time", title: "...if between [start time]..."
        input "endTime", "time", title: "...and [end time]."
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

    subscribe(theMotionSensor, "motion.active", motionActiveHandler)
    subscribe(theMotionSensor, "motion.inactive", motionInactiveHandler)
}

def motionActiveHandler(evnt) {
    log.debug "motionActiveHandler($evnt)"

    def start = timeToday(startTime)
    def end = timeToday(endTime)
    def presence = thePresenceSensor.currentValue("presence")

    log.debug "Presence: $presence, start.time: $start.time, end.time: $end.time"

    if (presence == "present" && start.time <= now() && now() <= end.time ) {
        turnOn()
    }
}

def motionInactiveHandler(evnt) {
    log.debug "motionInactiveHandler($evnt)"

    runIn((motionSensorTimeout * 60), checkMotion)
}

def checkMotion() {
    log.trace "checkMotion()"

    def motionState = theMotionSensor.currentState("motion")

    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.date.time
        def threshold = 1000 * ( ( 60 * motionSensorTimeout ) - 1 )

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check (${elapsed} ms): turn off switch"

            turnOff()
        } else {
            log.debug "Motion has not stayed inactive long enough since last check (${elapsed} ms): do nothing"
        }
    } else {
        log.debug "Motion is active: do nothing."
    }
}

def turnOn() {
    log.trace "turnOn(), state: ${state}"

    if (!state.switchOn) {
        log.debug "Turning on switch..."
        theSwitch.on()
        state.switchOn = true
    } else {
        log.debug "Switch already on"
    }
}

def turnOff() {
    log.trace "turnOff(), state: ${state}"

    if (state.switchOn) {
        log.debug "Turning off switch..."
        theSwitch.off()
        state.switchOn = false
    } else {
        log.debug "Switch already off"
    }
}