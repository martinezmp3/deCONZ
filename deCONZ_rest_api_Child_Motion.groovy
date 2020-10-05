/* 
child driver fo deCONZ_rest_api Motion
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
        09/26/20 intial release 
        09/29/20 add suport for motion sensor and Lights debug
*/

metadata {
    definition (name: "deCONZ_rest_api_Child_Motion", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
		capability "Motion Sensor"
		capability "Sensor"
        attribute "dark", "bool"
        attribute "battery", "float"
		attribute "lastUpdated", "String"
        attribute "ID", "String"
        command "SETdeCONZname"
        command "GETdeCONZname"
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}

def SETdeCONZname(){
    parent.PutRequest("sensors/${getDataValue("ID")}","{\"name\": \"${device.getLabel()}\"}")
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
    if (data)  sendEvent(name: "motion", value: "active")
    if (!data)  sendEvent(name: "motion", value: "inactive")
}
def updateLastUpdated (date){
    if (logEnable) log.debug "Last Update:${date}"
    sendEvent(name: "lastUpdated", value: date)
}
