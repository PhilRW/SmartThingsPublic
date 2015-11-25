/**
 *  Nest Auto Away
 *
 *  Copyright 2014 Samuel Wang
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
    name: "Nest Auto Away",
    namespace: "",
    author: "Samuel Wang",
    description: "Nest changes Home/Away status based on Presence.",
    category: "My Apps",
    iconUrl: "https://dl.dropboxusercontent.com/u/2421186/nestaway.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2421186/nestaway%402x.png")


preferences {
  section("When I arrive and leave..."){
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}

  section("Change these thermostats modes...") {
    input "thermostats", "capability.thermostat", multiple: true
  }
}

def installed()
{
	subscribe(presence1, "presence", presenceHandler)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", presenceHandler)
}

def presenceHandler(evt)
{
	log.debug "presenceHandler $evt.name: $evt.value"
	def current = presence1.currentValue("presence")
	log.debug current
	def presenceValue = presence1.find{it.currentPresence == "present"}
	log.debug presenceValue
	if(presenceValue){
		thermostats?.present()
		log.debug "Nest is set to Home."
	}
	else{
		thermostats?.away()
		log.debug "Nest is set to Away."
	}
}