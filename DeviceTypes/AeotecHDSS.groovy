/* 
 *	Aeotec Heavy Duty Smart Switch
 *
 *  Copyright 2015 Elastic Development
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  The latest version of this file can be found at:
 *  https://github.com/jpansarasa/SmartThings/blob/master/DeviceTypes/AeotecHDSS.groovy
 *
 *  Revision History
 *  ----------------
 *
 *  2015-01-17: Version: 1.0.0
 *  Initial Revision
 *	
 *	Developers Notes:
 * 	Raw Description	0 0 0x1001 0 0 0 12 0x5E 0x25 0x32 0x31 0x27 0x2C 0x2B 0x70 0x85 0x59 0x56 0x72 0x86 0x7A 0x73 0x98 0xEF 0x5A
 *
 *	Z-Wave Supported Command Classes:
 *	Code	Name								Version
 *	====	===================================	=======
 *	0x25	COMMAND_CLASS_SWITCH_BINARY			V1
 *	0x32	COMMAND_CLASS_METER					V3
 *	0x31	COMMAND_CLASS_SENSOR_MULTILEVEL		V5
 *	0x27	COMMAND_CLASS_SWITCH_ALL			V1
 *	0x70	COMMAND_CLASS_CONFIGURATION			V1
 *	0x56	COMMAND_CLASS_CRC_16_ENCAP			V1
 *	----	--- Supported but unimplemented ---	--
 *	0x2C	COMMAND_CLASS_SCENE_ACTUATOR_CONF	V1
 *	0x2B	COMMAND_CLASS_SCENE_ACTIVATION		V1
 *	0x85	COMMAND_CLASS_ASSOCIATION			V2
 *	0x72	COMMAND_CLASS_MANUFACTURER_SPECIFIC	V2
 *	0x86	COMMAND_CLASS_VERSION				V2
 *	0x7A	COMMAND_CLASS_FIRMWARE_UPDATE_MD	V2
 *	0x73	COMMAND_CLASS_POWERLEVEL			V1
 *	0x98	COMMAND_CLASS_SECURITY				V1
 *	0xEF	COMMAND_CLASS_MARK					V1
 *	----	--- Supported but unknown types ---	--
 *	0x5E	???
 *	0x59	???
 *	0x5A	???
 **/

metadata {
    definition (name: "Aeotec Heavy Duty Smart Switch", namespace: "elasticdev", author: "James P") {
        capability "Switch"
        capability "Energy Meter"
        capability "Power Meter"
        capability "Temperature Measurement"
        capability "Configuration"
        capability "Sensor"
		capability "Actuator"
        capability "Polling"
        capability "Refresh"
        
        attribute "voltage", "number"
        attribute "current", "number"

        command "reset"
       

        fingerprint deviceId: "0x1001", inClusters: "0x5E,0x25,0x32,0x31,0x27,0x2C,0x2B,0x70,0x85,0x59,0x56,0x72,0x86,0x7A,0x73,0x98,0xEF,0x5A"
    }

    // simulator metadata
    simulator {
        status "on":  "command: 2003, payload: FF"
        status "off": "command: 2003, payload: 00"

        for (int i = 0; i <= 10000; i += 1000) {
            status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
            scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
        }
        for (int i = 0; i <= 100; i += 10) {
            status "energy  ${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
            scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
        }

        // reply messages
        reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
        reply "200100,delay 100,2502": "command: 2503, payload: 00"

    }

    // tile definitions
    tiles {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on",	 label: '${name}', action: "switch.off", icon: "st.switches.switch.on",  backgroundColor: "#79b821"
            state "off", label: '${name}', action: "switch.on",  icon: "st.switches.switch.off", backgroundColor: "#ffffff"
        }
        valueTile("power", "device.power", decoration: "flat") {
            state "default",  label: '${currentValue} W'
        }
        valueTile("energy", "device.energy", decoration: "flat") {
            state "default", label:'${currentValue} kWh'
        }
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat") {
            state "default", label:'reset kWh', action:"reset"
        }
        standardTile("configure", "device.power", inactiveLabel: false, decoration: "flat") {
            state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main (["switch","power","energy"])
        details(["switch","power","energy","reset","configure","refresh"])
    }
    
    
    preferences {
        input "reportInterval", "number", title: "Report Interval", description: "The time interval in minutes for sending device reports", defaultValue: 1, required: false, displayDuringSetup: true
        input "switchAll", "enum", title: "Respond to switch all?", description: "How does the switch respond to the 'Switch All' command", options:["Disabled", "Off Enabled", "On Enabled", "On And Off Enabled"], defaultValue: "On And Off Enabled", required:false, displayDuringSetup: true
        input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: false, required: false, displayDuringSetup: true
    }
}

/********************************************************************************
 *	Methods																		*
 ********************************************************************************/

/**
 *	parse - Called when messages from a device are received from the hub
 *
 *	The parse method is responsible for interpreting those messages and returning Event definitions.
 *
 *	String	description		The message from the device
 */
def parse(String description) {
    if (debugOutput) log.debug "Parse(description: \"${description}\")"

    def event = null
    
    // The first parameter is the description string
    // The second parameter is a map that specifies the version of each command to use
    def cmd = zwave.parse(description, [0x20: 1, 0x25 : 1, 0x32 : 3, 0x31 : 5, 0x27 : 1, 0x2C : 1, 0x2B : 1, 0x70 : 1, 0x85 : 2, 0x56 : 1, 0x72 : 2, 0x86 : 2,  0x7A : 2, 0x73 : 1, 0x98 : 1,  0xEF : 1])

    if (cmd) {
        event = createEvent(zwaveEvent(cmd))
    }
    if (debugOutput) log.debug "Parse returned ${event?.inspect()}"
    return event
}

/**
 *	secure - sends a secure zwave response
 */
private secure(physicalgraph.zwave.Command cmd) {
    response(zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format())
}


/**
 *	on - Turns on the switch
 *
 *	Required for the "Switch" capability
 */
def on() {
	delayBetween([
    	secure(zwave.basicV1.basicSet(value: 0xFF)),
        secure(zwave.switchBinaryV1.switchBinaryGet())
	])
}

/**
 *	off - Turns off the switch
 *
 *	Required for the "Switch" capability
 */
def off() {
	delayBetween([
    	secure(zwave.basicV1.basicSet(value: 0x00)),
        secure(zwave.switchBinaryV1.switchBinaryGet())
	])
}

/**
 *	poll - Polls the device
 *
 *	Required for the "Polling" capability
 */
def poll() {
    delayBetween([
    	secure(zwave.basicV1.basicGet()),
	    secure(zwave.switchBinaryV1.switchBinaryGet()),
	    secure(zwave.sensormultilevelv5.SensorMultilevelGet(sensorType: 1, scale: 1)),
    	secure(zwave.meterV3.meterGet(scale: 0)), //kWh
    	secure(zwave.meterV3.meterGet(scale: 2)), //Wattage
    	secure(zwave.meterV3.meterGet(scale: 4)), //Voltage
    	secure(zwave.meterV3.meterGet(scale: 5)), //Current
    ])
}

/**
 *	refresh - Refreshed values from the device
 *
 *	Required for the "Refresh" capability
 */
def refresh() {
	delayBetween([
    	secure(zwave.basicV1.basicGet()),
		secure(zwave.switchBinaryV1.switchBinaryGet()),
	    secure(zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1, scale: 1)),
    	secure(zwave.meterV3.meterGet(scale: 0)), //kWh
    	secure(zwave.meterV3.meterGet(scale: 2)), //Wattage
    	secure(zwave.meterV3.meterGet(scale: 4)), //Voltage
    	secure(zwave.meterV3.meterGet(scale: 5)), //Current
	])
}

/**
 * 	reset - Resets the devices energy usage meter
 *
 *	Defined by the custom command "reset"
 */
def reset() {
	return [
		secure(zwave.meterV3.meterReset()),
    	secure(zwave.meterV3.meterGet(scale: 0)) //kWh
	]
}

/**
 *	configure - Configures the parameters of the device
 *
 *	Required for the "Configuration" capability
 */
def configure() {
    log.debug "configure(reportInterval:${reportInterval}, switchAll: ${switchAll})"

	//Get the values from the preferences section
    def reportIntervalSecs = 60;
    if (reportInterval) {
        reportIntervalSecs = 60 * reportInterval.toInteger()
    }
	
    def switchAllMode = physicalgraph.zwave.commands.switchallv1.SwitchAllSet.MODE_INCLUDED_IN_THE_ALL_ON_ALL_OFF_FUNCTIONALITY
    if (switchAll == "Disabled") {
        switchAllMode = physicalgraph.zwave.commands.switchallv1.SwitchAllSet.MODE_EXCLUDED_FROM_THE_ALL_ON_ALL_OFF_FUNCTIONALITY
    }
    else if (switchAll == "Off Enabled") {
        switchAllMode = physicalgraph.zwave.commands.switchallv1.SwitchAllSet.MODE_EXCLUDED_FROM_THE_ALL_ON_FUNCTIONALITY_BUT_NOT_ALL_OFF
    }
    else if (switchAll == "On Enabled") {
        switchAllMode = physicalgraph.zwave.commands.switchallv1.SwitchAllSet.MODE_EXCLUDED_FROM_THE_ALL_OFF_FUNCTIONALITY_BUT_NOT_ALL_ON
    }

	/***************************************************************
	Device specific configuration parameters
	----------------------------------------------------------------
	Param	Size	Default		Description
	0x03	1		0			Current Overload Protection (0=disabled, 1=enabled)
	0x14	1		0			LED status after power on: (0=last status, 1=always on, 2=always off)
	0x50	1		0			Enable to send notifications to associated devices in Group 1 when load changes (0=nothing, 1=hail CC, 2=basic CC report)
	0x5A	1		0			Enables/disables parameter 0x5B and 0x5C (0=disabled, 1=enabled)
	0x5B	2		50			The value here represents minimum change in wattage for a REPORT to be sent (Valid values 0‐ 60000)
	0x5C	1		10			Enables/disables parameter 0x5B and 0x5C (0=disabled, 1=enabled)
	0x64	1		N/A			Set 0x65-0x67 to default
	0x65	4		4			Which reports need to send in Report group 1
	0x66	4		8			Which reports need to send in Report group 2
	0x67	4		0			Which reports need to send in Report group 3
	0x6E	1		N/A			Set 0x6F-0x71 to default.
	0x6F	4		5			The time interval in seconds for sending Report group 1 (Valid values 0x01‐0x7FFFFFFF).
	0x70	4		120			The time interval in seconds for sending Report group 2 (Valid values 0x01‐0x7FFFFFFF).
	0x71	4		120			The time interval in seconds for sending Report group 3 (Valid values 0x01‐0x7FFFFFFF).
	0xC8	1		0			Partner  ID (0= Aeon Labs Standard Product, 1= Others).
	0xFC	1		0			Enable/Disable Lock Configuration (0 =disable, 1 = enable).
	0xFF	1		N/A			Reset to factory default setting
	0xFF	4		0x55555555	Reset to factory default setting and removed from the z‐wave network

	Configuration Values for parameters 0x65-0x67:
	BYTE  |	7	6	5	4	3	2	1	0
	=====================================
	MSB 0 |	0	0	0	0	0	0	0	0
	Val 1 |	0	0	0	0	0	0	0	0
	VAL 2 |	0	0	0	0	0	0	0	0
	LSB	3 |	0	0	0	0	A	B	C	D

	Bit A - Auto send Meter REPORT (for kWh) at the group time interval
	Bit B - Auto send Meter REPORT (for watt) at the group time interval
	Bit C - Auto send Meter REPORT (for current) at the group time interval
	Bit D - Auto send Meter REPORT (for voltage) at the group time interval

	Example - Send meter report for watt and voltage at group time interval
		value is 0x0005 
	Example - Send meter report for kWh and current at group time interval
		value is 0x000A or 10 (decimal)
	Example - Send meter report for all values at group time interval
		value is 0x000F or 15 (decimal)
	***************************************************************/
	log.debug "configure(reportIntervalSecs: ${reportIntervalSecs}, switchAllMode: ${switchAllMode})"
	delayBetween([
		secure(zwave.switchAllV1.switchAllSet(mode: switchAllMode)),
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0xFC, size: 1, scaledConfigurationValue: 0)),	//Disable Lock Configuration (0 =disable, 1 = enable).
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0x50, size: 1, scaledConfigurationValue: 2)),	//Enable to send notifications to associated devices when load changes (0=nothing, 1=hail CC, 2=basic CC report)
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0x5A, size: 1, scaledConfigurationValue: 1)),	//Enables parameter 0x5B and 0x5C (0=disabled, 1=enabled)
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0x5B, size: 2, scaledConfigurationValue: 50)),	//Minimum change in wattage for a REPORT to be sent (Valid values 0‐ 60000)
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0x5C, size: 1, scaledConfigurationValue: 10)),	//Minimum change in percentage for a REPORT to be sent (Valid values 0‐ 100)
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0x65, size: 4, scaledConfigurationValue: 15)),	//Which reports need to send in Report group 1
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0x6F, size: 4, scaledConfigurationValue: reportIntervalSecs)),	//Send Report to group 1 for this interval (Valid values 0x01‐0x7FFFFFFF).
		secure(zwave.configurationV1.configurationSet(parameterNumber: 0xFC, size: 1, scaledConfigurationValue: 1))		//Enable Lock Configuration (0 =disable, 1 = enable).
	])
}

/********************************************************************************
 *	Event Handlers																*
 ********************************************************************************/

/**
 * 	Default event handler -  Called for all unhandled events
 */
def zwaveEvent(physicalgraph.zwave.Command cmd) {
    if (debugOutput) {
    	log.debug "Unhandled: $cmd"
        createEvent(descriptionText: "${device.displayName}: ${cmd}")
    }
    [:]
}

/**
 *	COMMAND_CLASS_CRC_16_ENCAP (0x56)
 *
 *	List<Short>	commandByte
 *	Short		commandClassIdentifier
 *	Short		commandIdentifier
 *	List<Short>	initializationVector
 *	List<Short>	messageAuthenticationCode
 *	Short		receiversNonceIdentifier
 *	Boolean		secondFrame
 *	Short		sequenceCounter
 *	Boolean		sequenced
 */
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    // Devices that support the Security command class can send messages in an encrypted form
    // they arrive wrapped in a SecurityMessageEncapsulation command and must be unencapsulated
    // Like zwave.parse, the parameter is a map that can specify command class versions here like in zwave.parse
    def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x25 : 1, 0x32 : 3, 0x31 : 5, 0x27 : 1, 0x2C : 1, 0x2B : 1, 0x70 : 1, 0x85 : 2, 0x56 : 1, 0x72 : 2, 0x86 : 2,  0x7A : 2, 0x73 : 1, 0x98 : 1,  0xEF : 1])
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    }
}

/**
 *	COMMAND_CLASS_BASIC (0x20)
 *
 *	Short		value	0xFF for on, 0x00 for off
 */
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) 
{
	if (debugOutput) log.debug "BasicSet(value:${cmd.value})"
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical", displayed: true, isStateChange: true]
}

/**
 *	COMMAND_CLASS_BASIC (0x20)
 *
 *	Short		value
 */
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	if (debugOutput) log.debug "BasicReport(value:${cmd.value})"
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

/**
 *	COMMAND_CLASS_SWITCH_BINARY (0x25)
 *
 *	Short		value	0xFF for on, 0x00 for off
 */
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinarySet cmd)
{
	if (debugOutput) log.debug "SwitchBinarySet(value:${cmd.value})"
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital", displayed: true, isStateChange: true]
}

/**
 *	COMMAND_CLASS_SWITCH_BINARY (0x25)
 *
 *	Short		value	0xFF for on, 0x00 for off
 */
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	if (debugOutput) log.debug "SwitchBinaryReport(value:${cmd.value})"
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

/**
 *	COMMAND_CLASS_METER (0x32)
 *
 * 	Integer		deltaTime					Time in seconds since last report
 *	Short		meterType					Unknown = 0, Electric = 1, Gas = 2, Water = 3
 *	List<Short>	meterValue					Meter value as an array of bytes
 * 	Double		scaledMeterValue			Meter value as a double
 *	List<Short>	previousMeterValue			Previous meter value as an array of bytes
 *	Double		scaledPreviousMeterValue	Previous meter value as a double
 *	Short		size						The size of the array for the meterValue and previousMeterValue
 *	Short		scale						The scale of the values: "kWh"=0, "kVAh"=1, "Watts"=2, "pulses"=3, "Volts"=4, "Amps"=5, "Power Factor"=6, "Unknown"=7
 *	Short		precision					The decimal precision of the values
 *	Short		rateType					???
 *	Boolean		scale2
 */
def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
	def meterTypes = ["Unknown", "Electric", "Gas", "Water"]
    def electricNames = ["energy", "energy", "power", "count",  "voltage", "current", "powerFactor",  "unknown"]
    def electricUnits = ["kWh",    "kVAh",   "W",     "pulses", "V",       "A",       "Power Factor", ""]

	//NOTE: scaledPreviousMeterValue does not always contain a value
    if (debugOutput) log.debug "MeterReport(deltaTime:${cmd.deltaTime} secs, meterType:${meterTypes[cmd.meterType]}, meterValue:${cmd.scaledMeterValue}, previousMeterValue:${cmd.scaledPreviousMeterValue}, scale:${electricNames[cmd.scale]}(${cmd.scale}), precision:${cmd.precision}, rateType:${cmd.rateType})"
	

    def previousValue = cmd.scaledPreviousMeterValue
   
    def map = [ name: electricNames[cmd.scale], unit: electricUnits[cmd.scale], displayed: false]
    switch(cmd.scale) {
    	case 0: //kWh
        	previousValue = device.currentValue("energy")
            map.value = cmd.scaledMeterValue
            break;
        case 1: //kVAh
            map.value = cmd.scaledMeterValue
            break;
        case 2: //Watts
        	previousValue = device.currentValue("power")
            map.value = Math.round(cmd.scaledMeterValue)
            break;
        case 3: //pulses
            map.value = Math.round(cmd.scaledMeterValue)
            break;
        case 4: //Volts
        	previousValue = device.currentValue("voltage")
            map.value = cmd.scaledMeterValue
            break;
        case 5: //Amps
        	previousValue = device.currentValue("current")
            map.value = cmd.scaledMeterValue
            break;
        case 6: //Power Factor
        case 7: //Unknown
            map.value = cmd.scaledMeterValue
            break;
        default:
            break;
    }
    //Check if the value has changed my more than 5%, if so mark as a stateChange
    map.isStateChange = ((cmd.scaledMeterValue - previousValue).abs() > (cmd.scaledMeterValue * 0.05))
    
    createEvent(map)
}

/**
 *	COMMAND_CLASS_SENSOR_MULTILEVEL  (0x31)
 *	
 *	Short	sensorType	Supported Sensor: 0x01 (temperature Sensor)
 *	Short	scale		Supported scale:  0x00 (Celsius) and 0x01 (Fahrenheit)   
 */
def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd)
{
    if (debugOutput) log.debug "SensorMultilevelReport(sensorType:${cmd.sensorType}, scale:${cmd.scale}, precision:${cmd.precision}, scaledSensorValue:${cmd.scaledSensorValue}, sensorValue:${cmd.sensorValue}, size:${cmd.size})"
    //The temperature sensor only measures the internal temperature of product (Circuit board)
    if (cmd.sensorType == physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport.SENSOR_TYPE_TEMPERATURE_VERSION_1) {
		createEvent(name: "temperature", value: cmd.scaledSensorValue, unit: cmd.scale ? "F" : "C", displayed: false )
    }
}
//EOF