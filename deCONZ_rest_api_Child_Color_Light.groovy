/* 
Child_Light driver fo deCONZ_rest_api 
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
*/
import hubitat.helper.ColorUtils

metadata {
    definition (name: "deCONZ_rest_api_Child_Color_Light", namespace: "jorge.martinez", author: "Jorge Martinez", importUrl: "https:") {
		capability "Bulb"
        capability "Light"
//        capability "ColorMode"
        capability "ColorControl"
//        capability "ColorTemperature"
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
        command "GETdeCONZname"
        command "SETdeCONZname" , ["string"]
        command "changeID" , ["string"]
    }
}
preferences {
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
}



//**************///this is an adaptation from smartthing code of (Copyright 2017 Pedro Garcia) code original code could be found at https://github.com/puzzle-star
def setColor(red, green, blue) {
    if (logEnable) {
       // log.debug ("SetColor hue:${Color.hue} saturation:${Color.saturation} level:${Color.level}")
    }

    def colorName = ColorUtils.rgbToHEX([Math.round(red*255).intValue(), Math.round(green*255).intValue(), Math.round(blue*255).intValue()])
    setColorName("Setting Color\n[$colorName]");
   
    def xy = colorRgb2Xy(red, green, blue);
  
    //logTrace "setColor: xy ($xy.x, $xy.y)"
  
    def intX = Math.round(xy.x*65536).intValue() // 0..65279
    def intY = Math.round(xy.y*65536).intValue() // 0..65279
    
//********adaptaion to hubitat/deCONZ My code   
    def strX = intX.toString().padLeft(5, "0")
    def strY = intY.toString().padLeft(5, "0")
    log.debug "setColor: xy (0.${strX}, 0.${strY})"
    parent.PutRequest("lights/${getDataValue("ID")}/state","{\"xy\":[0.${strX},0.${strY}]}")
//********end of My code   
}
def setColor(Map colorMap) {
    log.debug "setColor: ${colorMap}"
    def rgb
    if(colorMap.containsKey("red") && colorMap.containsKey("green") && colorMap.containsKey("blue")) {
        rgb = [ red : colorMap.red.intValue() / 255, green: colorMap.green.intValue() / 255, blue: colorMap.blue.intValue() / 255 ]
    }
    else if(colorMap.containsKey("hue") && colorMap.containsKey("saturation")) {
        rgb = colorHsv2Rgb(colorMap.hue / 100, colorMap.saturation / 100)
    }
    else {
        log.warn "Unable to set color $colorMap"
    }

  //logTrace "setColor: RGB ($red, $green, $blue)"
    setColor(rgb.red, rgb.green, rgb.blue)
}

def colorHsv2Rgb(h, s) {
  //logTrace "< Color HSV: ($h, $s, 1)"
    
  def r
    def g
    def b
    
    if (s == 0) {
        r = 1
        g = 1
        b = 1
    }
    else {
        def region = (6 * h).intValue()
        def remainder = 6 * h - region

        def p = 1 - s
        def q = 1 - s * remainder
        def t = 1 - s * (1 - remainder)

    if(region == 0) {
            r = 1
            g = t
            b = p
        }
        else if(region == 1) {
            r = q
            g = 1
            b = p
        }
        else if(region == 2) {
            r = p
            g = 1
            b = t
        }
        else if(region == 3) {
            r = p
            g = q
            b = 1
        }
        else if(region == 4) {
            r = t
            g = p
            b = 1
        }
        else {
            r = 1
            g = p
            b = q
        }
  }
    
  //logTrace "< Color RGB: ($r, $g, $b)"
  
  [red: r, green: g, blue: b]
}
def colorRgb2Xy(r, g, b) {

  //logTrace "> Color RGB: ($r, $g, $b)"
  
  r = colorGammaAdjust(r)
  g = colorGammaAdjust(g)
  b = colorGammaAdjust(b)

  // sRGB, Reference White D65
  // D65  0.31271 0.32902
  //  R  0.64000 0.33000
  //  G  0.30000 0.60000
  //  B  0.15000 0.06000
  def M = [
    [  0.4123866,  0.3575915,  0.1804505 ],
    [  0.2126368,  0.7151830,  0.0721802 ],
    [  0.0193306,  0.1191972,  0.9503726 ]
  ]

  def X = r * M[0][0] + g * M[0][1] + b * M[0][2]
  def Y = r * M[1][0] + g * M[1][1] + b * M[1][2]
  def Z = r * M[2][0] + g * M[2][1] + b * M[2][2]
  
  //logTrace "> Color XYZ: ($X, $Y, $Z)"
  
  def x = X / (X + Y + Z)
  def y = Y / (X + Y + Z)
  
  //logTrace "> Color xy: ($x, $y)"

  [x: x, y: y]
}
def setColorName(name){
    log.debug "Color Name: ${name}"
    sendEvent(name: "colorName", value: name)
}

def colorGammaAdjust(component) {
  return (component > 0.04045) ? Math.pow((component + 0.055) / (1.0 + 0.055), 2.4) : (component / 12.92)
}
//*********//// end of Pedro Garcia code

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
    if (logEnable) log.debug "update updatePower on == ${status}"
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
