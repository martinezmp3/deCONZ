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
    definition (name: "deCONZ_rest_api_Parent", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
        capability "Initialize"
        capability "Refresh"
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
        device.updateSetting("WebSocketPort",response.json.websocketport)
        device.updateSetting("Notifyall",response.json.websocketnotifyall)
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
    if (logEnable) log.debug "Connection status: ${status}"
    
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
    if (logEnable) log.debug "PutRequest"
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
    if (logEnable) log.debug "addChildCallBack"
        if (response.hasError()){
        log.error "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        json = response.getJson()
        if (json.state.buttonevent){
            if (logEnable) log.debug "no error creating Button = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Button", "child-${json.uniqueid}", [name: "Button(${json.uniqueid})", label: json.name, ID: data["dataID"], isComponent: false])
        }
        if (json.state.presence){
            if (logEnable) log.debug "no error creating Motion = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Motion", "child-${json.uniqueid}", [name: "Motion(${json.uniqueid})", label: json.name, ID: data["dataID"], isComponent: false])
        }
        if (json.type.toString().contains("light")){
            if (logEnable) log.debug "no error creating Ligth = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Light", "child-${json.uniqueid}", [name: "Light(${json.uniqueid})", label: json.name, ID: data["dataID"], isComponent: false])
        }
        if (json.type.toString().contains("plug-in")){
            if (logEnable) log.debug "no error creating Ligth = ${json.name}"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Light", "child-${json.uniqueid}", [name: "plug-in(${json.uniqueid})", label: json.name, ID: data["dataID"], isComponent: false])
        }
    }
}
def parse(String description) {
    if (logEnable) log.debug "parse call"
    json = null
    try{
        json = new groovy.json.JsonSlurper().parseText(description)
        children = getChildDevice("child-${json.uniqueid}")
        if (logEnable) log.debug "recive: ${json}"    
        if(json == null){
            log.warn "String description not parsed"
            return
        }
        
        if (!children && json.r != "groups"){
            log.warn "Children NOT found creating one### ${json.r}/${json.id}"
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
//    children = getChildDevice("child-${json.uniqueid}")
    if (json.state && json.state.buttonevent){
        children.reciveData(json.state.buttonevent)
        if (logEnable) log.debug "Update for ${children.getLabel()}"
        if (json.state.lastupdated) children.updateLastUpdated(json.state.lastupdated)
    }
    if (json.state && json.state.presence!=NULL){
        children.updateMotion(json.state.presence)
        if (logEnable) log.debug "Update for ${children.getLabel()} active = ${json.state.presence}"
        if (json.state.lastupdated) children.updateLastUpdated(json.state.lastupdated)
    }
    if (json.state && json.r =="lights"){
        
        if (logEnable) log.debug "Update for ${children.getLabel()} on = ${json.state.on}"
        children.updatePower(json.state.on)
        if (json.state.bri) children.updateBri(json.state.bri)
        if (json.state.colormode) children.updateColormode(json.state.colormode)
        if (json.state.ct) children.updateCt(json.state.ct)
    }
    
    if (json.config && json.config.battery){
        if (logEnable) log.debug "Battery Update for child-${json.uniqueid} value:${json.config.battery}"
        if (children){
            children.updateBattery(json.config.battery)
        }
        if (!children) log.warn "Unable to update battery children not avalinble"
    }
//    log.debug description
      
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
    initialize()
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
}
def close (){
    interfaces.webSocket.close()
}
