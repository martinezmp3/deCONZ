/* 
Parent driver fo deCONZ_rest_api 
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
        09/25/20 intial release 
        09/26/20 add suport for motion sensor and Lights debug
        09/27/20 add autodiscover after creation bug fix and clde cleaing  
*/

metadata {
    definition (name: "deCONZ_rest_api_Child_Light", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
		capability "Bulb"
        capability "Light"
        capability "ColorMode"
        capability "ColorControl"
        capability "ColorTemperature"
        capability "SwitchLevel"
        capability "Switch"
        attribute "colorName", "String" 
        attribute "colorTemperature", "Number"
        attribute "colormode", "String" 
        attribute "bri", "Number"
 		attribute "lastUpdated", "String"
        attribute "ID", "String"
//        command "updateStatus", ["bool"]
//        command "setLevel", ["Number"]
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}
def on() {
    if (logEnable) log.debug "ON"
    sendEvent(name: "switch", value: "on")
    parent.PutRequest("lights/${getDataValue("ID")}/state",'{"on": true}')
}
def off() {
    if (logEnable) log.debug "OFF"
    sendEvent(name: "switch", value: "off")
    parent.PutRequest("lights/${getDataValue("ID")}/state",'{"on": false}')
}

def updateColormode (updateValue) {
    if (logEnable) log.debug "update Colormode: ${updateValue}"
    sendEvent(name: "colormode", value: updateValue)
}
def updateBri (updateValue) {
    if (logEnable) log.debug "update updateBri: ${updateValue}"
    sendEvent(name: "bri", value: updateValue)
    Newlevel = Math.round(updateValue*100/254)
    sendEvent(name: "level", value: Newlevel)
}
def updateCt (updateValue) {
    if (logEnable) log.debug "update colorTemperature: ${updateValue}"
    sendEvent(name: "colorTemperature", value: updateValue)
}
def updatePower(status) {
    if (logEnable) log.debug "update updatePower on == ${updateValue}"
    if (status) sendEvent(name: "switch", value: "on")
    if (!status) sendEvent(name: "switch", value: "off")
}
def setLevel (Percent){
    Value = Math.round(Percent/100*254)
    if (logEnable) log.debug "Value is: ${Value}/Percent is: ${Percent}"
    //state.level = Percent
    sendEvent(name: "level", value: Percent, isStateChange: true)
    parent.PutRequest("lights/${getDataValue("ID")}/state","{\"bri\":${Value}}")
}
def setColorTemperature (colortemperature){
    if (logEnable) log.debug "setColorTemperature: ${colortemperature}"
    sendEvent(name: "colorTemperature", value: colortemperature, isStateChange: true)
    parent.PutRequest("lights/${getDataValue("ID")}/state","{\"ct\":${colortemperature}}")
}
