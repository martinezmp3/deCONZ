/* 
Child_Light driver fo deCONZ_rest_api 
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
        05/07/21 Creation
*/

metadata {
    definition (name: "deCONZ_rest_api_Child_Window", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
		capability "WindowBlind"
        capability "WindowShade"
        capability "SwitchLevel"
        capability "Switch"
        capability "TemperatureMeasurement"
        attribute "bri", "Number"
 		attribute "lastUpdated", "String"
        attribute "ID", "String"
        attribute "lift", "Number"
        attribute "battery", "Number"
        attribute "temperature", "Number"
        command "GETdeCONZname"
        command "SETdeCONZname" , ["string"]
        command "changeID" , ["string"]
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}
def updatePower(status) {
    if (logEnable) log.debug "update updatePower on == ${status}"
    if (status) sendEvent(name: "switch", value: "on")
    if (!status) {
        sendEvent(name: "switch", value: "off")
        sendEvent(name: "windowShade", value: "open")
        sendEvent(name: "position", value: 0)
        sendEvent(name: "lift", value: 0)
        sendEvent(name: "level", value:0)
        
    }
}
def updateBri (updateValue) {
    if (logEnable) log.debug "update updateBri: ${updateValue}"
    sendEvent(name: "bri", value: updateValue)
    
}
def updateOpen(updateValue) {
    if (logEnable) log.debug "update Open: ${updateValue}"
//    sendEvent(name: "open", value: updateValue)
//    if (updateValue) sendEvent(name: "windowShade", value: "open")
//    if (!updateValue) sendEvent(name: "windowShade", value: "close")
}

def updateLift(updateValue) {
    if (state.Previouslift == NULL) state.Previouslift = updateValue
    if (logEnable) log.debug "update Lift: ${updateValue}"
    sendEvent(name: "lift", value: updateValue)
    sendEvent(name: "level", value: updateValue, isStateChange: true)
    sendEvent(name: "position", value: updateValue, isStateChange: true)
    if (state.Previouslift>updateValue) sendEvent(name: "windowShade", value: "opening")
    if (state.Previouslift<updateValue) sendEvent(name: "windowShade", value: "closing")
    if (updateValue == 100) sendEvent(name: "windowShade", value: "close")
    state.Previouslift = updateValue
}
def changeID (ID){
    updateDataValue("ID",ID)
}
def SETdeCONZname(name){
    if (name==null) name = device.getLabel()
    parent.PutRequest("lights/${getDataValue("ID")}","{\"name\": \"${name}\"}")
}
def GETdeCONZname(){
    parent.updateCildLabel(getDataValue("ID"),false)
}
def updateBattery (bat){
    if (logEnable) log.debug "new batt:${bat}"
    sendEvent(name: "battery", value: bat)
}
def on() {
    if (logEnable) log.debug "ON"
    sendEvent(name: "switch", value: "on")
    parent.PutRequest("lights/${getDataValue("ID")}/state",'{"open": true}')
}
def off() {
    if (logEnable) log.debug "OFF"
    sendEvent(name: "switch", value: "off")
    parent.PutRequest("lights/${getDataValue("ID")}/state",'{"open": false}')
}
def setPosition(position){
    //setLevel (position)
    parent.PutRequest("lights/${getDataValue("ID")}/state","{\"lift\":${position}}")
}
def open(){
    parent.PutRequest("lights/${getDataValue("ID")}/state",'{"open": true}')
}
def close(){
    parent.PutRequest("lights/${getDataValue("ID")}/state",'{"open": false}')
}
def stop (){
    parent.PutRequest("lights/${getDataValue("ID")}/state","{\"lift\":\"stop\"")
}
def setLevel (Percent){
    if (logEnable) log.debug "Value is: ${Value}/Percent is: ${Percent}"
    sendEvent(name: "position", value: Percent, isStateChange: true)
//    sendEvent(name: "level", value: Percent, isStateChange: true)
    parent.PutRequest("lights/${getDataValue("ID")}/state","{\"lift\":${Percent}}")
}
