/**
 *  Summer Fan Occupancy + Temperature Controller
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
    name: "Summer Fan Occupancy + Temperature Controller",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Sets fan speed based on motion and temperature",
    category: "My Apps",
    iconUrl:   "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png")


preferences {
    section("Set fan controls:") {
        input "theThermostat", "capability.temperatureMeasurement", title: "Thermostat:", multiple: false, required: true
        input "theMotionSensor", "capability.motionSensor", title: "Motion sensor:", multiple: false, required: true
        input "switches", "capability.switchLevel", title: "Fan(s) to control:", multiple: true, required: true
        input "motionSensorTimeout", "number", title: "Off after this many minutes:", required: true
       	input "lowThreshold", "decimal", title: "On low if at or above:", defaultValue: 22.5, required: false
       	input "mediumThreshold", "decimal", title: "On medium if at or above:", defaultValue: 25, required: false
       	input "highThreshold", "decimal", title: "On high if at or above:", defaultValue: 27.5, required: false
   	}
}


def installed() 
{
	log.info "Summer Fan Occupancy Controller: ${settings}"
	initialize()
}

def updated() 
{
	log.trace "Summer Fan Occupancy Controller - updated()"
    unsubscribe()
    initialize()
}

def initialize() 
{
	log.trace "Summer Fan Occupancy Controller - initialize()"
    subscribe(theMotionSensor, "motion.active", motionDetectedHandler)
    subscribe(theMotionSensor, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    def temp = theThermostat.currentState("temperature")
    log.debug "Motion detected. Temp is ${temp.integerValue}. Low/Med/Hi = ${lowThreshold}/${mediumThreshold}/${highThreshold}"
    if (temp.integerValue >= highThreshold) {
        log.debug "Setting level to high, 99"
        switches.setLevel(99);
    } else if (temp.integerValue >= mediumThreshold) {
        log.debug "Setting level to medium, 67"
        switches.setLevel(67);
    } else if (temp.integerValue >= lowThreshold) {
        log.debug "Setting level to low, 33"
        switches.setLevel(33);
    } else {
        log.debug "Temperature not within range, not turning on."
    }
}
 
def motionStoppedHandler(evt) { 
    log.debug "motionStoppedHandler called: $evt"
    runIn(60 * motionSensorTimeout, checkMotion)
}

def checkMotion() {
    log.debug "In checkMotion scheduled method"
    
    def motionState = theMotionSensor.currentState("motion")

    if (motionState.value == "inactive") {
        def elapsed = now() - motionState.date.time
        def threshold = 1000 * 60 * motionSensorTimeout

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms): turning switch off"
            log.debug "Setting level to off, 0"
            switches.setLevel(0)
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms): doing nothing"
        }
    } else {
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}
