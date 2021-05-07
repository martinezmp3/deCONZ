/* 
Parent driver fo deCONZ_rest_api 
This driver is to control the deCONZ_rest_api from the hubitat hub. 
I wrote this diver for personal use. If you decide to use it, do it at your own risk. 
No guarantee or liability is accepted for damages of any kind. 
        09/25/20 intial release 
        09/25/20 doubleTap(button) (report it by @Royski)
        09/26/20 add suport for motion sensor and Lights 
        09/27/20 add autodiscover after creation bug fix and code cleaing
	09/28/20 import name from deCONZ on child creation (report it by @kevin)
	09/29/20 add connection drop recover (report it by@sburke781
        10/02/20 add reconect after reboot (report it by @sburke781)
        10/03/20 add refresh funtion call connect () (report it by @sburke781)
        10/04/20 save time and date of connection event/child button fix typo released (report it by@sburke781)
        10/04/20 auto rename from and to deCONZ
        10/05/20 custom name box on setdeCONZname funcion
        05/04/21 fix permit join 
        05/05/21 add State Variables zigbeechannel (request by @sburke781)
        05/06/21 add status, timeToRetry, timeoutCount to Current States variables to be access from rule machine (report it by @akafester)

*/

metadata {
    definition (name: "deCONZ_rest_api_Parent", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https://raw.githubusercontent.com/martinezmp3/deCONZ/master/deCONZ_rest_api_Parent.groovy") {
        capability "Initialize"
        capability "Refresh"
        attribute "timeoutCount", "Number"
        attribute "timeToRetry", "Number"
        attribute "status", "string"
        command "SendMsg", ["string"]
        command "close"
        command "discover"
        command "connect"
        command "GetApiKey"
        command "GetRequest",["string"]
        command "PutRequest",["string", "string"]
        command "GetConfiguration"
//        command "webSocketStatus"
        command "GetName"
        command "testCMD"
        command "SetTimeFormat", [[name:"Format", type: "ENUM", description: "Pick an option", constraints: ["12h","24h"] ] ]
        command "setPermitjoin" ,["number"]
        command "unlock",["number"]
    }
}
preferences {
    input("ip", "text", title: "Hub IP", description: "IP Address (in form of 192.168.0.5)", required: true)
    input("port", "text", title: "Hub API port", description: "Api port", required: true)
    input("API_key", "text", title: "APi key", description: "")
    input("WebSocketPort", "text", title: "Web Socket Port", description: "")
    input name: "Notifyall", type: "bool", title: "Web sock notif all clients"
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    
}
def SetTimeFormat (timeFormat){
    PutRequest('config',"{\"timeformat\": ${timeFormat}}")
}
def setPermitjoin (time){
    if (!time) time = 60
    PutRequest('config',"{\"permitjoin\": ${time}}")
    if (logEnable) log.debug "gateway is set to allow new devices to join for ${time} seconds"
    
}
def unlock(time){
    if (!time) time = 60
    PutRequest('config',"{\"unlock\": ${time}}")
    if (logEnable) log.debug "gateway is set to be unlock for ${time} seconds"
}
def testCMD (){
    sendHubCommand(new hubitat.device.HubAction("lan discovery urn:schemas-upnp-org:device:MediaRenderer:1", hubitat.device.Protocol.LAN))
}
def updateCildLabel (ID,Sensor){
    if (logEnable) log.debug "updatitng childs label"
    type = "lights"
    if (Sensor) type = "sensors"
    log.debug type
    def getParams = [
        uri: "http://${settings.ip}:${settings.port}/api/${settings.API_key}/${type}/${ID}",
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
		//body : ["devicetype" : "Hubitat"]
	]
    asynchttpGet("updateCildLabelCallBack",getParams)
}
def updateCildLabelCallBack (response, data){
    if (logEnable) log.debug "updateCildLabelCallBack ${data}"
    if (response.hasError()){
        log.error "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        if (logEnable) log.debug "no error responce = ${response.getData()}"
        json = response.getJson()
        children = getChildDevice("child-${json.uniqueid}")
        if (logEnable) log.debug "changing lable from ${children.getLabel()} to ${json.name}"
        children.setLabel(json.name)
        //log.debug response.json.websocketport
    }
}
def GetConfiguration (){
    if (logEnable) log.debug "Updating configuration"
    def getParams = [
        uri: "http://${settings.ip}:${settings.port}/api/${settings.API_key}/config",
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : ["devicetype" : "Hubitat"]
	]
    asynchttpGet("GetConfigurationCallBack",getParams)
}
def GetConfigurationCallBack (response, data){
    if (logEnable) log.debug "GetConfigurationCallBack"
    if (response.hasError()){
        log.debug "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        log.debug "no error"// responce = ${response.getData()}"
        log.debug "websocketport  = ${response.json.websocketport}"
        log.debug "websocketnotifyall = ${response.json.websocketnotifyall}"
        log.debug "zigbeechannel = ${response.json.zigbeechannel}"
        device.updateSetting("WebSocketPort",response.json.websocketport)
        device.updateSetting("Notifyall",response.json.websocketnotifyall)
        state.zigbeechannel = response.json.zigbeechannel
    }
}
def processCallBack(response, data) {
    if (logEnable) log.debug "processCallBack"
    if (response.hasError()){
        log.error "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        if (logEnable) log.debug "no error responce = ${response.getData()}"
        //log.debug response.json.websocketport
    }
}
def GetApiKeyCallBack(response, data) {
    if (logEnable) log.debug "GetApiKeyCallBack"
    if (response.hasError()){
        log.debug "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        log.debug "no error ${response.getData()}"
        strData = response.getData()
        start = strData.indexOf("username")+9
        end = strData.indexOf('\"',start)
        log.debug ("${start+2} to end ${end}")
        APIkey = strData.substring(start+2,start+12)
        log.debug APIkey
        device.updateSetting("API_key",APIkey)
    }
}
def webSocketStatus(String status){
    if (state.timeoutCount == null || state.timeToRetry == null){
        state.timeoutCount = 0
        state.timeToRetry = 10
        sendEvent(name: "timeoutCount", value: 0)
        sendEvent(name: "timeToRetry", value: 10)
    }
    if (logEnable) log.debug "Connection status: ${status}"
    if (status.contains("open")){
        state.timeoutCount = 0
        state.timeToRetry = 10
        sendEvent(name: "timeoutCount", value: 0)
        sendEvent(name: "timeToRetry", value: 10)
        state.status = "OK"
        sendEvent(name: "status", value: "OK")
        def Day = new Date().format("MMM dd yy", location.timeZone)
        def Now = new Date().format("h:mm a", location.timeZone)
        state.message = "OK since ${Day} at ${Now}"
        if (logEnable) log.debug state.message
        
    }
    if (status.contains("failure")){
        if (state.status == "OK"){
            def Day = new Date().format("MMM dd yy", location.timeZone)
            def Now = new Date().format("h:mm a", location.timeZone)
            state.message = "tring to reconnect since ${Day} at ${Now}"
            state.status = "attempting reconnect"
            sendEvent(name: "status", value: "attempting reconnect")
        }
        state.timeoutCount += 1
        if ((state.timeoutCount == 50) && (state.timeToRetry == 10)){ //try to connect every 10 sec 50 times
            state.status = "warning"
            sendEvent(name: "status", value: "warning")
            state.timeToRetry = 30
            sendEvent(name: "timeToRetry", value: 30)
        }
        if ((state.timeoutCount == 100) && (state.timeToRetry == 30)){ //try to connect every 30 sec 50 times
            state.status = "critical warning"
            sendEvent(name: "status", value: "critical warning")
            state.timeToRetry = 1800
            sendEvent(name: "timeToRetry", value: 1800)
        }
        if ((state.timeoutCount == 150) && (state.timeToRetry == 1800)){ //try to connect every 1/2 hour 50 times
            state.status = "error"
            sendEvent(name: "status", value: "error")
            state.timeToRetry = 3600
            sendEvent(name: "timeToRetry", value: 3600)
        }
        if ((state.timeoutCount == 200) && (state.timeToRetry == 3600)){ //try to connect every 1 hour 50 times
            state.status = "critical error"
            sendEvent(name: "status", value: "critical error")
            sendEvent(name: "timeToRetry", value: 5400)
            state.timeToRetry = 5400
        }
        if (state.timeoutCount == 250){
            state.status = "fail"
            sendEvent(name: "status", value: "fail")
        }
        log.error "conection problem: ${status} retry in ${state.timeToRetry} second atemp(${state.timeoutCount})"
        if ((state.status != "fail") && (settings.ip !=null) && (settings.WebSocketPort!=null)){
            runIn(state.timeToRetry,"connect")
        }
    }   
}
def GetApiKey (){
    if (logEnable) log.debug "GetApiKey"
    def postParams = [
		uri: "http://${settings.ip}:${settings.port}/api/",
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : ["devicetype" : "Hubitat"]
	]
    asynchttpPost("GetApiKeyCallBack",postParams)
}
def GetName (){
    request = "lights/4"
    def getParams = [
        uri: "http://${settings.ip}:${settings.port}/api/${settings.API_key}/${request}",
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : ["devicetype" : "Hubitat"]
	]
    response = NULL
    asynchttpGet("processCallBack",getParams,$response)
    log.debug "this is what i got: ${response}"
    
}
def GetRequest (String request){
    if (logEnable) log.debug "GetRequest"
    def getParams = [
        uri: "http://${settings.ip}:${settings.port}/api/${settings.API_key}/${request}",
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : ["devicetype" : "Hubitat"]
	]
    asynchttpGet("processCallBack",getParams)
}
def PutRequest (String request, String body){ 
    if (logEnable) log.debug "PutRequest: request:${request}, body ${body}"
    url = "http://${settings.ip}:${settings.port}/api/${settings.API_key}/${request}"
    log.debug ("uri = ${url} body = ${body}")
    def getParams = [
        uri: url,
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
//        body : '{"on": false}'
		body : body
	]
    asynchttpPut("processCallBack",getParams)
}
def discover(){
    if (logEnable) log.debug "Discovering hub"
    try {
        httpGet("https://dresden-light.appspot.com/discover") { resp ->
            if (resp.success) {
                strData = resp.data.toString()
                start = strData.indexOf("internalipaddress")+18
                end = strData.indexOf(',',start)
                internalipaddress = strData.substring(start,end)
                start = strData.indexOf("internalport")+13
                end = strData.indexOf(',',start)
                internalport = strData.substring(start,end)
                device.updateSetting("ip",internalipaddress)
                device.updateSetting("port",internalport)
                if (logEnable) log.debug "server connection= ${internalipaddress}:${internalport}"
                
            }
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}
def addChildCallBack (response, data){  ///[dataID: json.uniqueid]
    if (logEnable) log.debug "Adding Child"
        if (response.hasError()){
        log.error "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        json = response.getJson()
        if (logEnable) log.debug json.type.toString()
        if (json.state.buttonevent){
            if (logEnable) log.debug "no error creating Button = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Button", "child-${json.uniqueid}", [name: "Button(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        if (json.type.toString().contains("ZHAPresence") || json.type.toString().contains("ZHAWater")){
            if (logEnable) log.debug "no error creating Motion = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Motion", "child-${json.uniqueid}", [name: "Motion(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        if (json.type.toString().contains("light")){
            if (logEnable) log.debug "no error creating Ligth = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Light", "child-${json.uniqueid}", [name: "Light(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        if (json.type.toString().contains("plug-in")){
            if (logEnable) log.debug "no error creating Ligth = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Light", "child-${json.uniqueid}", [name: "plug-in(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        if (json.type.toString().contains("On/Off output")){
            if (logEnable) log.debug "no error creating Ligth = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Light", "child-${json.uniqueid}", [name: "plug-in(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        ///addition
        if (json.type.toString().contains("ZHAPower")){
            if (logEnable) log.debug "no error creating On/Off = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Light", "child-${json.uniqueid}", [name: "ZHAPower(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        if (json.type.toString().contains("OpenClose")){
            if (logEnable) log.debug "no error creating Ligth = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Contact", "child-${json.uniqueid}", [name: "contact-sensor(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        if (json.type.toString().contains("Temperature")){
            if (logEnable) log.debug "no error creating Temperature = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Temperature", "child-${json.uniqueid}", [name: "Temperature(${json.uniqueid})", label: json.name, ID: data["dataID"],manufacturername: json.manufacturername,modelid: json.modelid,type: json.type, isComponent: false])
        }
        //end of addition
    }
}
def parse(String description) {
    if (logEnable) log.debug "parse call"
    json = null
    children = null
    try{
        json = new groovy.json.JsonSlurper().parseText(description)
        children = getChildDevice("child-${json.uniqueid}")
        if (logEnable) log.debug "recive: ${json}"    
        if(json == null){
            log.warn "String description not parsed"
            return
        }
        if ((!children && json.r != "groups") && !json.attr ){ //json.attr.modelid !="RaspBee" ){        
            log.warn "Children NOT found creating one ${json.r}/${json.id}"
            def getParams = [
            uri: "http://${settings.ip}:${settings.port}/api/${settings.API_key}/${json.r}/${json.id}",
            headers: ["Accept": "application/json, text/plain, */*"],
		    requestContentType: 'application/json',
		    contentType: 'application/json',
		    //body : ["devicetype" : "Hubitat"]
	        ]
            asynchttpGet("addChildCallBack",getParams, [dataID: json.id])
            children = getChildDevice("child-${json.uniqueid}")
        }
    }  catch(e) {
        log.error("Failed to parse json e = ${e}")
        return
    }
    if (json.state && json.state.buttonevent){
        children.reciveData(json.state.buttonevent)
        if (logEnable) log.debug "Update for ${children.getLabel()}"
        if (json.state.lastupdated) children.updateLastUpdated(json.state.lastupdated)
        if (json.state.lowbattery!=NULL) children.sendEvent(name:"lowbattery", value: json.state.lowbattery, isStateChange: true)
    }
    if (json.state && (json.state.presence!=NULL || json.state.water!=NULL)){  //tremporary fix for smartthing motion sensor
        
        if (json.state.presence!=NULL) children.updateMotion(json.state.presence)
        
        if (json.state.water!=NULL) children.updateMotion(json.state.water)
        
        if (logEnable && json.state.presence!=NULL) log.debug "Update for ${children.getLabel()} active = ${json.state.presence}"
        
        if (logEnable && json.state.water !=NULL) log.debug "Update for ${children.getLabel()} active = ${json.state.water}"
        
        if (json.state.lastupdated) children.updateLastUpdated(json.state.lastupdated)
        if (json.state.lowbattery!=NULL) children.sendEvent(name:"lowbattery", value: json.state.lowbattery, isStateChange: true)
    }
    if (json.state && json.r =="lights"){
        if (logEnable) log.debug "Update for ${children.getLabel()} on = ${json.state.on}"
        children.updatePower(json.state.on)
        if (json.state.bri) children.updateBri(json.state.bri)
        if (json.state.colormode) children.updateColormode(json.state.colormode)
        if (json.state.ct) children.updateCt(json.state.ct)
    }
    
    if (json.state && json.state.open!=NULL){
        if (json.state.open) children.sendEvent(name:"contact", value: "open", isStateChange: true)
        if (!json.state.open) children.sendEvent(name:"contact", value: "close", isStateChange: true)
        if (json.state.tampered) children.sendEvent(name:"tamper", value: "detected", isStateChange: true)
        if (!json.state.tampered) children.sendEvent(name:"tamper", value: "clear", isStateChange: true)
        if (logEnable) log.debug "Update for ${children.getLabel()} open = ${json.state.open}"
        if (json.state.lastupdated) children.sendEvent(name:"lastUpdated", value: json.state.lastupdated, isStateChange: true)
        if (json.state.lowbattery!=NULL) children.sendEvent(name:"lowbattery", value: json.state.lowbattery, isStateChange: true)
    }
    if (json.state && json.state.temperature){
        int tempC = (json.state.temperature.toInteger()/100)
        children.sendEvent(name:"temperature", value: celsiusToFahrenheit(tempC), isStateChange: true)
        if (json.state.lowbattery!=NULL) children.sendEvent(name:"lowbattery", value: json.state.lowbattery, isStateChange: true)
    }
    if (json.config && json.config.battery){
        if (logEnable) log.debug "Battery Update for child-${json.uniqueid} value:${json.config.battery}"
        if (children){
            children.updateBattery(json.config.battery)
        }
        if (!children) log.warn "Unable to update battery children not avalinble"
    }
}
def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
def installed() {
    if (logEnable) log.debug "installed() called"
    discover()
    //updated()
}
def configure() {
    discover()
}
def updated() {
    if (logEnable) log.info "updated() called"
    unschedule()
    close()
    connect()
}
def connect (){
    if (!ip) {
        log.warn "Hub IP Address not set"
        return
    }
    try {
        interfaces.webSocket.connect("ws://${settings.ip}:${settings.WebSocketPort}/")//Connect the webSocket
    } 
    catch(e) {
        if (logEnable) log.debug "initialize error: ${e.message}"
        log.error "WebSocket connect failed"
    }
}
def initialize() {
    if (logEnable) log.info "initialize() called"
    connect ()
}
def close (){
    interfaces.webSocket.close()
}
def refresh(){
    close ()
    connect ()
}
