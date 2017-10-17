definition(
    name: "Zwave Switch Indicator Light Manager",
    namespace: "PhilRW",
    author: "Philip Rosenberg-Watt",
    description: "Changes the indicator light setting to always be off",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)


preferences {
    section("When these switches are toggled, adjust the indicator...") { 
        input "mains", "capability.switch", 
            multiple: true, 
            title: "Switches to fix:", 
            required: true
    }
}


def installed(){
    subscribe(mains, "switch.on", switchOnHandler)
    subscribe(mains, "switch.off", switchOffHandler)
}


def updated() {
    unsubscribe()

    subscribe(mains, "switch.on", switchOnHandler)
    subscribe(mains, "switch.off", switchOffHandler)

    log.info "subscribed to all of switches events"
}


def switchOffHandler(evnt) {
    log.info "switchoffHandler Event: ${evnt.value}"

    mains?.indicatorWhenOn()
}


def switchOnHandler(evnt) {
    log.info "switchOnHandler Event: ${evnt.value}"

    mains?.indicatorWhenOff()
}