/**
*  Gentle Wake Up Snooze
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
    name: "Gentle Wake Up Snooze",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Snooze by turning off one or more switches temporarily.",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom2-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom2-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom2-icn@2x.png")

preferences {
    section("Settings") {
        input "theSwitch", "capability.switch", title:  "Activate snooze with this switch:"
        input "gentleWakeUpController", "capability.switch", title: "Gentle Wake Up Controller:"
        input "snoozeSwitchesOff", "capability.switch", title: "Turn off these switches:", required: false, multiple: true
        input "snoozeMinutes", "number", title: "Turn on Gentle Wake Up Controller after this many minutes:", defaultValue: 10
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
    subscribe(theSwitch, "switch.on", snoozeOnHandler)
    subscribe(theSwitch, "switch.off", snoozeOffHandler)
}


def snoozeOnHandler(evnt) {
    log.trace "snoozeOnHandler($evnt)"

    //TODO: Add run only when Gentle Wake Up is active (need support in GWU controller)
    //def wakingUp = gentleWakeUpController.currentValue("switch")
    //log.trace "wakingUp = ${wakingUp}"

    if (!state.snoozing) {
        //if (wakingUp == "on") {
        log.info "Snoozing for ${snoozeMinutes} minutes..."
        gentleWakeUpController.off()
        snoozeSwitchesOff.off()
        state.snoozing = true
        runIn(60 * snoozeMinutes, endSnooze)
        //} else {
        //log.debug "Gentle Wake Up is not active, ignoring input and resetting switch..."
        //theSwitch.off()
        //}
    } else {
        log.debug "Snooze is active, cannot start new snooze until current snooze ends."
    }
}


def snoozeOffHandler(evnt) {
    log.trace "snoozeOffHandler($evnt)"

    if (state.snoozing) {
        log.info "Snooze cancelled. Gentle Wake Up will not start."
        state.snoozing = false
    } else {
        log.debug "Snooze is not active, cannot cancel."
    }
}



def endSnooze() {
    log.trace "endSnooze()"

    if (state.snoozing) {
        log.debug "Snooze is finished, turning ${gentleWakeUpController.name} back on..."
        gentleWakeUpController.on()
        state.snoozing = false
        theSwitch.off()
    } else {
        log.debug "Snooze is not active, cannot end snooze."
    }
}