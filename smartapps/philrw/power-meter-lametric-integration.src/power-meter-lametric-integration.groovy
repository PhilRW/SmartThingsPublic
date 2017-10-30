/**
*  Power Meter RGB Bulb Controller
*
*  Copyright 2017 Philip Rosenberg-Watt
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
    name: "Power Meter LaMetric Integration",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Update LaMetric app with latest power meter reading",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Title") {
        input "theMeter", "capability.powerMeter", title: "Meter to monitor:", required: true
        input "showChart", "bool", title: "Enable history graph?", defaultValue: false, submitOnChange: true
    }
}


def installed() {
    log.trace "Installed with settings: ${settings}"

    initialize()
}


def updated() {
    log.trace "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}


def initialize() {
    log.trace "initialize()"

    state.maxPower = 0
    state.minPower = 0

    subscribe(theMeter, "power", powerHandler)
    if (showChart) {
        state.ints = new int[37]
        runEvery1Minute(updateChart)
    }
}


def powerHandler(evnt) {
    log.trace "powerHandler(${evnt.value})"

    def curPower = new BigDecimal(evnt.value)

    log.debug "curPower: ${curPower} (${state.minPower} - ${state.maxPower})"

    if (curPower > state.maxPower) {
        log.trace "new max power ${curPower}, previous ${state.maxPower}"
        state.maxPower = curPower
    } else if (curPower < state.minPower) {
        log.trace "new min power ${curPower}, previous ${state.minPower}"
        state.minPower = curPower
    }

    def pct = 0
    if (curPower > 0) {
        pct = curPower / state.maxPower
    } else {
        pct = (0 - curPower) / state.minPower
    }
    log.debug "pct = ${pct}"

    def iconRed = "i14432"
    def iconGreen = "i14431"
    def iconSel = ""
    if (curPower <= 0) {
        log.trace "selecting green icon"
        iconSel = iconGreen
    } else {
        log.trace "selecting red icon"
        iconSel = iconRed
    }

    state.frame0 = [
        goalData: [
            start: Math.round(state.minPower),
            current: Math.round(curPower),
            end: Math.round(state.maxPower),
            unit: "W"
        ],
        icon: iconSel,
        index: 0
    ]

    sendUpdate()
}


def updateChart() {
    log.trace "updateChart()"

    int curPower = Math.round(theMeter.currentPower)
    log.trace("updateChart() curPower: ${curPower}")

    log.trace "state.ints[] pre-shift: ${state.ints}"
    for (int i = 37-1; i > 0; i--) {                
        state.ints[i] = state.ints[i-1];
    }
    state.ints[0] = curPower
    log.trace "state.ints[] post-shift: ${state.ints}"

    def points = new int[37]
    for (int i = 0; i < 37; i++) {
        points[i] = state.ints[i] + Math.abs(Math.round(state.minPower))
    }
    log.debug "points[]: ${points}"

    state.frame1 = [
        index: 1,
        chartData: points
    ]

    sendUpdate()

}


def sendUpdate() {
    log.trace "sendUpdate()"

    def myFrames = [state.frame0]
    if (showChart) {
        myFrames = [state.frame0, state.frame1]
    }

    def params = [
        uri: "https://developer.lametric.com/api/v1/dev/widget/update/com.lametric.d0b2ac12744560fad7c3406b3c79f809/1",
        headers: ["X-Access-Token": "YmIxZTViOTAxNzhhNDJmZTYyMDYxM2QzOWViMTBhOTc2NGMyN2VlMTBjYTRmNzIyN2E5MTI3MmE2MmVmMjM4Mw=="],
        body: [
            frames: myFrames
        ]
    ]

    log.debug "body: ${body}"

    try {
        httpPostJson(params) { resp ->
            log.trace "response status: ${resp.status}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}