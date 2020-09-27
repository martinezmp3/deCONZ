/* 
child driver fo deCONZ_rest_api Button
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
        09/26/20 intial release 
        09/29/20 add suport for motion sensor and Lights debug
*/

metadata {
    definition (name: "deCONZ_rest_api_Child_Button", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
        capability "PushableButton"
        capability "HoldableButton"
        capability "DoubleTapableButton"
        capability "ReleasableButton"
        command "hold" ,["NUMBER"]            ///   hold(<button number that was held>)
        command "push" ,["NUMBER"]            ///   push(<button number that was pushed>)
        command "doubleTap" ,["NUMBER"]            ///   doubleTap(<button number that was double tapped>)
        command "release" ,["NUMBER"]
        command "reciveData", ["string"]
        attribute "numberOfButtons", "NUMBER" ///	NUMBER	numberOfButtons		//sendEvent(name:"numberOfButtons", value:<number of physical buttons on the device>)	
        attribute "pushed", "NUMBER"          ///	NUMBER	pushed				//sendEvent(name:"pushed", value:<button number that was pushed>)
        attribute "held", "NUMBER"            ///   NUMBER	held				//sendEvent(name:"held", value:<button number that was held>)
        attribute "doubleTapped","NUMBER"	  ///                               //sendEvent(name:"doubleTapped", value:<button number that was double tapped>)
        attribute "battery", "float"
        attribute "lastUpdated", "String"
        attribute "ID", "String"
        
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}

def hold (button){
    if (logEnable) log.debug "button ${button} hold"
    sendEvent(name:"held", value:button, isStateChange: true)
}
def doubleTap (button){
    if (logEnable) log.debug "button ${button} doubleTap"
    sendEvent(name:"doubleTapped", value:button, isStateChange: true)
}
def push (button){
    if (logEnable) log.debug "button ${button} push"
    sendEvent(name:"pushed", value:button, isStateChange: true)
}

def release (button){
    if (logEnable) log.debug "button ${button} release"
    sendEvent(name:"release", value:button, isStateChange: true)
}

def updateLastUpdated (date){
    if (logEnable) log.debug "Last Update:${date}"
    sendEvent(name: "lastUpdated", value: date)
}


def updateBattery (bat){
    if (logEnable) log.debug "new batt:${bat}"
    sendEvent(name: "battery", value: bat)
}
def reciveData (data){
    int button = data.toInteger()/1000
    int action = data.toInteger() - button *1000
    if (logEnable) log.debug "button = ${button} action = ${action}"
    if (action == 1){
        hold(button)
    }
    if (action == 2){
        push(button)
    }
    if (action == 3){
        release(button)
    }
    if (action == 4){
        doubleTap(button)
    }
    //log.debug data
    
    if (button >  device.currentValue("numberOfButtons") ){
        if (logEnable) log.debug "updatating number of butoon from ${device.currentValue("numberOfButtons")} to ${button}"
        sendEvent(name:"numberOfButtons", value: button, isStateChange: true)
    }
}
