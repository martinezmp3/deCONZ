metadata {
    definition (name: "deCONZ_rest_api_Child_Button", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
        capability "PushableButton"
        capability "HoldableButton"
        capability "DoubleTapableButton"
        command "hold" ,["NUMBER"]            ///   hold(<button number that was held>)
        command "push" ,["NUMBER"]            ///   push(<button number that was pushed>)
        command "doubleTap" ,["NUMBER"]            ///   doubleTap(<button number that was double tapped>)
        command "reciveData", ["string"]
        attribute "numberOfButtons", "NUMBER" ///	NUMBER	numberOfButtons		//sendEvent(name:"numberOfButtons", value:<number of physical buttons on the device>)	
        attribute "pushed", "NUMBER"          ///	NUMBER	pushed				//sendEvent(name:"pushed", value:<button number that was pushed>)
        attribute "held", "NUMBER"            ///   NUMBER	held				//sendEvent(name:"held", value:<button number that was held>)
        attribute "doubleTapped","NUMBER"	  ///                               //sendEvent(name:"doubleTapped", value:<button number that was double tapped>)
        attribute "battery", "float"
        
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}

def hold (button){
    log.debug "button ${button} hold"
    sendEvent(name:"held", value:button)
}
def doubleTap (button){
    log.debug "button ${button} doubleTap"
    sendEvent(name:"doubleTapped", value:button)
}
def push (button){
    log.debug "button ${button} push"
    sendEvent(name:"pushed", value:button)
}
def updateBattery (bat){
    log.debug "new batt:${bat}"
    sendEvent(name: "battery", value: bat)
}
def reciveData (data){
    int button = data.toInteger()/1000
    int action = data.toInteger() - button *1000
    log.debug "button = ${button} action = ${action}"
    if (action == 1){
        hold(button)
    }
    if (action == 2){
        push(button)
    }
    if (action == 3){
//        push(button)
    }
    //log.debug data
    
    if (button >  device.currentValue("numberOfButtons") ){
        log.debug "updatating number of butoon from ${device.currentValue("numberOfButtons")} to ${button}"
        sendEvent(name:"numberOfButtons", value: button, isStateChange: true)
    }
}
