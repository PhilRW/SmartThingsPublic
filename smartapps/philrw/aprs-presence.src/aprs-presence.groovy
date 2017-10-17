/**
*  APRS Presence
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
    name: "APRS Presence",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Query APRS.fi for location of a single callsign to control a virtual presence sensor.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Settings") {
        input("callsign", "text", title: "Callsign", required: true)
        input("apikey", "text", title: "aprs.fi API Key", required: true)
        input("whoDat", "capability.presenceSensor", title: "Simulated Presence Sensor to control", required: true)
		input("threshold", "number", title: "Proximity in km", defaultValue: 0.1)
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
    queryAprs()
    runEvery15Minutes(queryAprs)
}

def queryAprs() {

    def params = [
        uri: "https://api.aprs.fi/api/get?name=${callsign}&what=loc&apikey=${apikey}&format=json",
        contentType: 'application/json'
    ]
    try {
        BigDecimal lat
        BigDecimal lon
        httpGet(params) { resp ->
            log.debug "response entries: ${resp.data.entries}"

            lat = new BigDecimal(resp.data.entries[0].lat)
            lon = new BigDecimal(resp.data.entries[0].lng)

            log.debug "response lat: ${lat}"
            log.debug "response lon: ${lon}"

        }

        def distance = calculateDistance(lat, lon, location.latitude, location.longitude)
        if (distance > threshold) {
            log.debug "too far away!"
            if (whoDat.currentPresence != "not present") { whoDat.departed() }
        } else {
            log.debug "close enough!"
            if (whoDat.currentPresence != "present") { whoDat.arrived() }
        }


    } catch (e) {
        log.error "something went wrong: $e"
    }
}

/**
* Calculate distance based on Haversine algorithmus
* (thanks to http://www.movable-type.co.uk/scripts/latlong.html)
*
* @param latitudeFrom
* @param longitudeFrom
* @param latitudeTo
* @param longitudeTo
* @return distance in Kilometers
*/
def calculateDistance(BigDecimal latitudeFrom, BigDecimal longitudeFrom,
                      BigDecimal latitudeTo, BigDecimal longitudeTo) {
    def EARTH_RADIUS = 6371

    log.debug "latFrom = " + latitudeFrom
    log.debug "lonFrom = " + longitudeFrom
    log.debug "latTo = " + latitudeTo
    log.debug "lonTo = " + longitudeTo

    def dLat = Math.toRadians(latitudeFrom - latitudeTo)
    def dLon = Math.toRadians(longitudeFrom - longitudeTo)

    //a = sin²(Δlat/2) + cos(lat1).cos(lat2).sin²(Δlong/2)
    //distance = 2.EARTH_RADIUS.atan2(√a, √(1−a))
    def a = Math.pow(Math.sin(dLat / 2), 2) +
        Math.cos(Math.toRadians(latitudeFrom)) *
        Math.cos(Math.toRadians(latitudeTo)) * Math.pow(Math.sin(dLon / 2), 2)
    def distance = 2 * EARTH_RADIUS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    log.debug "distance = " + distance

    return distance
}
