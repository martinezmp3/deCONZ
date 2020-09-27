# deCONZ
# This driver is to integrate deCONZ rest api  to hubitat 
# work in progress this is to use those trouble zig bee devices that hubitat is not 
# supporting because compatibility stability issues 
# like some of the ikea devices like the button dimmer and control. an acara buttons
# use at your onw risk like all my drivers I write it for my onw use and share for free
# I will work on this driver as long as hubitat do not support the devices I use if/or hubitat release an official support

You will need:
- deCONZ https://www.phoscon.de/en/conbee2/install
- the 4 drivers so far more comming

to install
- add the devices to your Phoscon-GW 
- add the 4 driver to your hub
- create a virtual driver with the parent driver
- on the device page click discover that will set the ip of the pi then refresh the page you will see the ip
- now you need the api key go to the hub page http://you_Phoscon_ip
- click the hamburger 
- Select gateway then advance then click on Authenticate app
- go back to your hubitat hub on the device page click Get Api Key refresh page you will se apy key 
- click get configuration then connect 
Finally press the any keys on the device and the parent driver will create a child per device press all the buttons on the devices 
and the parent will adjust how many button the device have. If you encounter any issue please post it no the habitat community page
