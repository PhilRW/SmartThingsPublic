/**
*  Summer Fan Temperature + Occupancy Controller
*
*  Copyright 2016 Philip Rosenberg-Watt
*
*	Version History
*
*   1.5     2017-10-17      Add winter mode, runs fan(s) on low for 24h since last motion, includes optional override switch
*	1.4		2016-06-04		Remove state tracking, instead check level of switch(es)
*	1.3		2016-05-23		Abstract out state tracking to separate method
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
    iconUrl:   "http://cdn.device-icons.smartthings.com/Lighting/light24-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light24-icn@2x.png",
)


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
            input "winterMode", "bool", title: "Enable winter mode (run on low for 24h)?", defaultValue: false, submitOnChange: true
            if (winterMode) {
                input "theWinterMotionSensor", "capability.motionSensor", title: "Winter motion sensor:", multiple: false, required: true
                input "winterSwitchOverride", "bool", title: "Use override switch?", defaultValue: false, submitOnChange: true
                if (winterSwitchOverride) {
                    input "theWinterSwitches", "capability.switch", title: "Turn off with switch(es) turn on:", multiple: true, required: true
                }
            } else {
                input "lowThreshold", "decimal", title: "On low if at or above:", defaultValue: 23, required: true
                input "mediumThreshold", "decimal", title: "On medium if at or above:", defaultValue: 25, required: true
                input "highThreshold", "decimal", title: "On high if at or above:", defaultValue: 27, required: true
                input "runOnOccupied", "bool", title: "Only run when room is occupied?", defaultValue: false, submitOnChange: true
                if (runOnOccupied) {
                    input "theMotionSensor", "capability.motionSensor", title: "Motion sensor:", multiple: false, required: true
                    input "motionSensorTimeout", "number", title: "Off after this many minutes:", required: true
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
    if (winterMode) {
        subscribe(theWinterMotionSensor, "motion", winterMotionHandler)
        if (winterSwitchOverride) {
            subscribe(theWinterSwitches, "switch.on", winterSwitchHandler)
        }
    } else {
        subscribe(theThermostat, "temperature", tempChangeHandler)
        if (runOnOccupied) {
            log.debug "runOnOccupied: ${runOnOccupied}, subscribing to motion handler..."
            subscribe(theMotionSensor, "motion", motionHandler)
        }
    }
}


def winterMotionHandler(evnt) {
    log.trace "winterHandler(${evnt}), value = ${evnt.value}"

    def override = false

    if (winterSwitchOverride) {
        theWinterSwitches.each {
            if (it.currentValue("switch") == "on") {
                override = true
            }
        }
    }

    if (override) {
        log.trace "Override active, not turning on fan."
    } else {
        setLevel("LOW")
        runIn(86400, winterCheckMotion)
    }
}


def winterSwitchHandler(evnt) {
    log.trace "winterSwitchHandler(${evnt}), value = ${evnt.value}"

    setLevel("OFF")
}


def winterCheckMotion() {
    log.trace "winterCheckMotion()"

    def motionState = theWinterMotionSensor.currentState("motion")
    def elapsed = now() - motionState.date.time
    def threshold = 1000 * ( 86400 - 100 )

    if (motionState.value == "inactive" && elapsed >= threshold) {
        log.debug "Motion has stayed inactive long enough since last check ($elapsed ms): turn fan(s) off"

        setLevel("OFF")
    } else {
        log.debug "Motion is active or not inactive long enough, leaving on"
    }
}



def tempChangeHandler(evnt) {
    log.trace "tempChangeHandler(${evnt}) - temperature update"
    def temp = evnt.doubleValue

    if (runOnOccupied) {
        checkMotion()
    } else {
        checkTemp(temp)
    }
}


def motionHandler(evnt) {
    log.trace "motionHandler(${evnt}), value = ${evnt.value}"
    if (runOnOccupied) {
        if (evnt.value == "active") {
            log.debug "motion active"
            def temp = theThermostat.currentState("temperature")
            checkTemp(temp.doubleValue)
        } else {
            log.debug "motion inactive"
            runIn(60 * motionSensorTimeout, checkMotion)
        }
    }
}


def setLevel(lvl) {
    log.trace "setLevel(${lvl})"

    def changeSwitches = switches.findAll { it.currentValue("currentState") != lvl }

    if (changeSwitches.size() > 0) {
        log.debug "Changing level to ${lvl}."
        if (lvl == "OFF") {
            switches.off()
        } else {
            switches.setLevel(lvl)
        }
    } else { 
        log.debug "Switch(es) already set to ${lvl}, doing nothing."
    }
}


def checkTemp(temp) {
    log.trace "checkTemp(${temp})"
    log.debug "Temp is ${temp}. Low/Med/Hi = ${lowThreshold}/${mediumThreshold}/${highThreshold}"

    if (temp >= highThreshold) {
        log.debug "Setting to high"
        setLevel("HIGH")
    } else if (temp >= mediumThreshold) {
        log.debug "Setting to medium"
        setLevel("MED")
    } else if (temp >= lowThreshold) {
        log.debug "Setting to low"
        setLevel("LOW")
    } else {
        log.debug "Temperature below range, setting to off"
        setLevel("OFF")
    }
}


def checkMotion() {
    log.trace "checkMotion()"

    def motionState = theMotionSensor.currentState("motion")
    def elapsed = now() - motionState.date.time
    def threshold = 1000 * ( ( 60 * motionSensorTimeout ) - 1 )

    if (motionState.value == "inactive" && elapsed >= threshold) {
        log.debug "Motion has stayed inactive long enough since last check ($elapsed ms): turn fan(s) off"

        setLevel("OFF")
    } else {
        log.debug "Motion is active or not inactive long enough, check temperature"
        def temp = theThermostat.currentState("temperature")

        checkTemp(temp.doubleValue)
    }
}
