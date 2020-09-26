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
    log.debug "Updating configuration"
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
    log.debug "GetConfigurationCallBack"
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
    log.debug "processCallBack"
    if (response.hasError()){
        log.debug "got error # ${response.getStatus()} ${response.getErrorMessage()}"
    }
    if (!response.hasError()){
        log.debug "no error responce = ${response.getData()}"
        //log.debug response.json.websocketport
    }
}
def GetApiKeyCallBack(response, data) {
    log.debug "GetApiKeyCallBack"
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
    log.debug status
}
def GetApiKey (){
    def postParams = [
		uri: "http://${settings.ip}:${settings.port}/api/",
        headers: ["Accept": "application/json, text/plain, */*"],
		requestContentType: 'application/json',
		contentType: 'application/json',
		body : ["devicetype" : "Hubitat"]
	]
    asynchttpPost("GetApiKeyCallBack",postParams)
}
def GetRequest (String request){
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
    log.debug "Discovering hub"
    if (logEnable) log.debug "Sending on GET request to [https://dresden-light.appspot.com/discover]"
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
def parse(String description) {
    def json = null;
    try{
        json = new groovy.json.JsonSlurper().parseText(description)
        log.debug "${json}"    
        if(json == null){
            log.warn "String description not parsed"
            return
        }
    }  catch(e) {
        log.error("Failed to parse json e = ${e}")
        return
    }
    //log.debug "parse call"
    if (json.state && json.state.buttonevent){
        log.debug json.uniqueid
        log.debug json.state.buttonevent
        def children = getChildDevice("child-${json.uniqueid}")
        if (!children){
            if (logEnable) log.debug "Children NOT found creating one"
            children = addChildDevice("jorge.martinez","deCONZ_rest_api_Child_Button", "child-${json.uniqueid}", [name: "Button(${json.uniqueid})", label: user, isComponent: false])
        }
        children.reciveData(json.state.buttonevent)
//        log.debug description
    }
//    log.debug description
      
}
def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}
def installed() {
    log.debug "installed() called"
    updated()
}
def updated() {
    log.info "updated() called"
    //Unschedule any existing schedules
    unschedule()
    initialize()
}
def connect (){
    if (!ip) {
        log.warn "Hub IP Address not set"
        return
    }
    try {
    interfaces.webSocket.connect("ws://${settings.ip}:443/")//Connect the webSocket
    } 
    catch(e) {
        if (logEnable) log.debug "initialize error: ${e.message}"
        log.error "WebSocket connect failed"
    }
}
def initialize() {
    log.info "initialize() called"
}
def close (){
    interfaces.webSocket.close()
}
