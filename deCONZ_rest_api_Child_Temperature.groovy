/* 
Child_Temperature driver fo deCONZ_rest_api 
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
        09/25/20 intial release 
        09/26/20 add suport for motion sensor and Lights debug
        09/27/20 add autodiscover after creation bug fix and code cleaing
	    09/28/20 import name from deCONZ on child creation
	    09/29/20 add connection drop recover 
        10/02/20 add reconect after reboot 
        10/03/20 add refresh funtion call connect () @sburke781
        10/04/20 save time and date of connection event/child button fix typo released @sburke781
*/

metadata {
    definition (name: "deCONZ_rest_api_Child_Temperature", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
		capability "TemperatureMeasurement"
        capability "Battery"
		capability "Sensor"
		attribute "lastUpdated", "String"
        attribute "ID", "String"
        attribute "lowbattery", "bool"  ///lowbattery:false
        attribute "temperature", "Number"
        command "GETdeCONZname"
        command "SETdeCONZname" , ["string"]
        command "changeID" , ["string"]
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    input name: "TemperatureC", type: "bool", title: "Enable temperature in C", defaultValue: false
}
def changeID (ID){
    updateDataValue("ID",ID)
}
def SETdeCONZname(name){
    if (name==null) name = device.getLabel()
    parent.PutRequest("sensors/${getDataValue("ID")}","{\"name\": \"${name}\"}")
}
def GETdeCONZname(){
    parent.updateCildLabel(getDataValue("ID"),true)
}
def updateBattery (bat){
    if (logEnable) log.debug "new batt:${bat}"
    sendEvent(name: "battery", value: bat)
}
def updateMotion (data){
    if (logEnable) log.debug "motion change :${data}"
    if (data)  sendEvent(name: "contact", value: "open")
    if (!data)  sendEvent(name: "contact", value: "close")
}
def updateLastUpdated (date){
    if (logEnable) log.debug "Last Update:${date}"
    sendEvent(name: "lastUpdated", value: date)
}
