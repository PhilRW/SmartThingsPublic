/**
 *  Set Mode via REST
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
    name: "Set Mode via REST",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Change mode using RESTful API",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Override Mode") {
    	input "overrideMode", "mode", title: "Ignore commands if in this mode (optional):"
    }
}

mappings {
  path("/mode") {
    action: [
      GET: "getMode"
    ]
  }
  path("/mode/:modeName") {
  	action: [
      PUT: "setMode"
    ]
  }
}

def getMode() {
    return [mode: location.currentMode.name]
}

void setMode() {
	if (location.currentMode != overrideMode) {
    	def modeName = params.modeName
    	def found = false
    	for (m in location.modes) {
    	 	log.debug("mode m: $m comparing with modeName $modeName")
    		if ("$modeName" == "$m") {
    	    	log.debug("Match found")
    	        found = true
    	    }
    	}

    	if (modeName) {
    		log.debug("modeName: $modeName")
    	    log.debug("location.modes: $location.modes")
    		if (!found) {
    	    	httpError(400, "$modeName is not a valid mode for this location")
    	    } else {
    	    	location.setMode(modeName)
    	    }
    	}
    } else {
    	httpError(500, "Mode override active, command ignored")
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
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers