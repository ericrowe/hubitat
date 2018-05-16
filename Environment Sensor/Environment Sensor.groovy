metadata {
    definition (name: "Environment Sensor", namespace: "iharyadi", author: "iharyadi", ocfDeviceType: "oic.r.temperature") {
        capability "Configuration"
        capability "Refresh"
        capability "Temperature Measurement"
        capability "RelativeHumidityMeasurement"
        capability "Illuminance Measurement"
        
        attribute "pressure", "string"

        fingerprint profileId: "0104", inClusters: "0000, 0003, 0006, 0402, 0403, 0405, 0400, 0B05", manufacturer: "KMPCIL", model: "RES001BME280", deviceJoinName: "Environment Sensor"
        }

    // simulator metadata
    simulator {
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "temperature", type: "generic", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
                attributeState "temperature", label: '${currentValue}°',
                        backgroundColors: [
                                [value: 31, color: "#153591"],
                                [value: 44, color: "#1e9cbb"],
                                [value: 59, color: "#90d2a7"],
                                [value: 74, color: "#44b621"],
                                [value: 84, color: "#f1d801"],
                                [value: 95, color: "#d04e00"],
                                [value: 96, color: "#bc2323"]
                        ]
            }
        }
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 3, height: 2, wordWrap: true) {
            state "humidity", label: 'Humidity ${currentValue}%', defaultState: true
        }
        valueTile("pressure", "device.pressure", inactiveLabel: false, width: 3, height: 2, wordWrap: true) {
            state "pressure", label: 'Pressure ${currentValue}kPa', defaultState: true
        }
        
        valueTile("illuminance", "device.illuminance", width:6, height: 2) {
            state "luminosity", label:'${currentValue} ${unit}', unit:"lux"
        }
        
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        def tiles_detail = [];
        tiles_detail.add("temperature")
        tiles_detail.add("humidity")
        tiles_detail.add("pressure")
        tiles_detail.add("illuminance")
        MapDiagAttributes().each{ k, v -> valueTile("$v", "device.$v", width: 2, height: 2, wordWrap: true) {
                state "val", label: "$v \n"+'${currentValue}', defaultState: true
            };
            tiles_detail.add(v);
        }
        tiles_detail.add("refresh")
                
        main "temperature"        
        details(tiles_detail)        
    }
    
    preferences {
        input "tempOffset", "decimal", title: "Degrees", description: "Adjust temperature by this many degrees",
              range: "*..*", displayDuringSetup: false
    }
    
    preferences {
        input "humOffset", "decimal", title: "%", description: "Adjust humidity by this many %",
              range: "*..*", displayDuringSetup: false
    }
}

private def NUMBER_OF_RESETS_ID()
{
    return 0x0000;
}

private def MAC_TX_UCAST_RETRY_ID()
{
    return 0x0104;
}

private def MAC_TX_UCAST_FAIL_ID()
{
    return 0x0105;
}

private def NWK_DECRYPT_FAILURES_ID()
{
    return 0x0115;
}

private def PACKET_VALIDATE_DROP_COUNT_ID()
{
    return 0x011A;
}

private def PARENT_COUNT_ID()
{
    return 0x011D+1;
}

private def CHILD_COUNT_ID()
{
    return 0x011D+2;
}

private def NEIGHBOR_COUNT_ID()
{
    return 0x011D+3;
}

private def LAST_RSSI_ID()
{
    return 0x011D;
}

private def DIAG_CLUSTER_ID()
{
    return 0x0B05;
}

private def TEMPERATURE_CLUSTER_ID()
{
    return 0x0402;
}

private def PRESSURE_CLUSTER_ID()
{
    return 0x0403;
}

private def HUMIDITY_CLUSTER_ID()
{
    return 0x0405;
}

private def ILLUMINANCE_CLUSTER_ID()
{
    return 0x0400;
}

private def SENSOR_VALUE_ATTRIBUTE()
{
    return 0x0000;
}

private def MapDiagAttributes()
{
    def result = [(CHILD_COUNT_ID()):'Children',
        (NEIGHBOR_COUNT_ID()):'Neighbor',
        (NUMBER_OF_RESETS_ID()):'ResetCount',
        (MAC_TX_UCAST_RETRY_ID()):'TXRetry',
        (MAC_TX_UCAST_FAIL_ID()):'TXFail',
        (LAST_RSSI_ID()):'RSSI',
        (NWK_DECRYPT_FAILURES_ID()):'DecryptFailure',
        (PACKET_VALIDATE_DROP_COUNT_ID()):'PacketDrop'] 

    return result;
}

private def createDiagnosticEvent( String attr_name, type, value )
{
    def result = [:]
    result.name = attr_name
    result.translatable = true
    
    def converter = [(DataType.INT8):{int val -> return (byte) val},
    (DataType.INT16):{int val -> return val},
    (DataType.UINT16):{int val -> return (long)val}] 
    
    result.value = converter[zigbee.convertHexToInt(type)]( zigbee.convertHexToInt(value));
    
    result.descriptionText = "{{ device.displayName }} $attr_name was $result.value"

    return createEvent(result)
}

private def parseDiagnosticEvent(def descMap)
{       
    def attr_name = MapDiagAttributes()[descMap.attrInt];
    if(!attr_name)
    {
        return null;
    }
    
    return createDiagnosticEvent(attr_name, descMap.encoding, descMap.value)
}

private def parsePressureEvent(def descMap)
{       
    if(zigbee.convertHexToInt(descMap.attrId) != SENSOR_VALUE_ATTRIBUTE())
    {
        return null
    }
    
    def result = [:]
    result.name = "pressure"
    result.translatable = true
    float press = (float)zigbee.convertHexToInt(descMap.value) / 10.0
    result.value = String.format("%.1f", press)
    result.descriptionText = "{{ device.displayName }} pressure was $result.value"
    return result
}

private def parseHumidityEvent(def descMap)
{       
    if(zigbee.convertHexToInt(descMap.attrId) != SENSOR_VALUE_ATTRIBUTE())
    {
        return null
    }
    
    def result = [:]
    result.name = "humidity"
    result.translatable = true
    result.value = (float)zigbee.convertHexToInt(descMap.value)/100.0
    result.descriptionText = "{{ device.displayName }} humidity was $result.value"
    return result
}

private def createIlluminanceEvent(int ilumm)
{
    def result = [:]
    result.name = "illuminance"
    result.translatable = true
    if(ilumm == 0)
    {
        result.value = 0.0
    }
    else
    {
        result.value = String.format("%.2f", 10 ** (((double) ilumm / 10000.0) -1.0))
    }
    result.descriptionText = "{{ device.displayName }} illuminance was $result.value"
    return result
}

private String ilummStringPrefix()
{
    return "illuminance: "
}

private def parseIlluminanceEventFromString(String description)
{
    if(!description.startsWith(ilummStringPrefix()))
    {
        return null
    }
    int ilumm = Integer.parseInt(description.substring(ilummStringPrefix().length()))
    
    return createIlluminanceEvent(ilumm)
}

def parseIlluminanceEvent(def descMap)
{       
    if(zigbee.convertHexToInt(descMap.attrId) != SENSOR_VALUE_ATTRIBUTE())
    {
        return null
    }
    
    int res =  zigbee.convertHexToInt(descMap.value)
    
    return createIlluminanceEvent(res)
}

def parseCustomEvent(String description)
{
    def event = null
    def descMap = zigbee.parseDescriptionAsMap(description)
    if(description?.startsWith("read attr - raw:"))
    {
        if(descMap?.clusterInt == DIAG_CLUSTER_ID())
        {
            event = parseDiagnosticEvent(descMap);
        }
        else if(descMap?.clusterInt == PRESSURE_CLUSTER_ID())
        {
            event = parsePressureEvent(descMap);
        }
        else if(descMap?.clusterInt == HUMIDITY_CLUSTER_ID())
        {
         	event = parseHumidityEvent(descMap); 
        }
        else if(descMap?.clusterInt == ILLUMINANCE_CLUSTER_ID())
        {
         	event = parseIlluminanceEvent(descMap); 
        }
   }
   return event
}

private String tempStringPrefix()
{
    return "temperature:"
}

private String humidityStringPrefix()
{
    return "humidity:"
}

private def createAdjustedTempString(double val)
{
    double adj = 0.0
    if (tempOffset) {
        adj = tempOffset
    }
    
    return tempStringPrefix() + " " +(val + adj).toString()
}

private def createAdjustedHumString(double val)
{
    double adj = 0.0
    if (humOffset) {
        adj = humOffset
    }
    
    return humidityStringPrefix() + " " +(val + adj).toString() + "%"
}

private def adjustTempValue(String description)
{
    
    if(description.startsWith(tempStringPrefix()))
    {
        double d = Double.parseDouble(description.substring(tempStringPrefix().length()))
        return createAdjustedTempString(d)
    }
   
    if(description.startsWith(humidityStringPrefix()))
    {
        double d = Double.parseDouble(description.substring(humidityStringPrefix().length()).replaceAll("[^\\d.]", ""))
        return createAdjustedHumString(d)
    }
    
    if(!description.startsWith("catchall:"))
    {
        return description
    }
    
    def descMap = zigbee.parseDescriptionAsMap(description)
    
    if(descMap.attrInt != SENSOR_VALUE_ATTRIBUTE())
    {
        return description
    }
    
    if( descMap.clusterInt == TEMPERATURE_CLUSTER_ID() )
    {
        return createAdjustedTempString((double) zigbee.convertHexToInt(descMap.value) / 100.00)
    }
    else if(descMap.clusterInt == HUMIDITY_CLUSTER_ID())
    {
        return createAdjustedHumString((double) zigbee.convertHexToInt(descMap.value) / 100.00)
    }
    
    return description 
 }

// Parse incoming device messages to generate events
def parse(String description) {
    
    description = adjustTempValue(description)
    log.debug "description is $description"
    
    def event = zigbee.getEvent(description)
    if(event)
    {
        sendEvent(event)
        return
    }
    
    event = parseIlluminanceEventFromString(description)
    if(event)
    {
        sendEvent(event)
        return
    }
    
    event = parseCustomEvent(description)
    if(event)
    {
        sendEvent(event)
        return
    }
    log.warn "DID NOT PARSE MESSAGE : $description"
}

def off() {
    zigbee.off()
}

def on() {
    zigbee.on()
}

def refresh() {
    log.debug "Refresh"
    def cmds = zigbee.readAttribute(TEMPERATURE_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE()) +
        zigbee.readAttribute(HUMIDITY_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE()) + 
        zigbee.readAttribute(PRESSURE_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE()) +
        zigbee.readAttribute(ILLUMINANCE_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE()) 
    MapDiagAttributes().each{ k, v -> cmds +=  zigbee.readAttribute(DIAG_CLUSTER_ID(), k) }
   
    return cmds
}

def configure() {
    log.debug "Configuring Reporting and Bindings."
    List cmds = zigbee.temperatureConfig(5,300)
    cmds = cmds + zigbee.configureReporting(HUMIDITY_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE(), DataType.UINT16, 5, 300, 100)
    cmds = cmds + zigbee.configureReporting(PRESSURE_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE(), DataType.UINT16, 5, 300, 2)
    cmds = cmds + zigbee.configureReporting(ILLUMINANCE_CLUSTER_ID(), SENSOR_VALUE_ATTRIBUTE(), DataType.UINT16, 1, 300, 500)
    cmds = cmds + refresh();
    return cmds
}

def updated() {
    log.trace "updated():"

    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) {
        state.updatedLastRanAt = now()
        
        return response(configure())
    }
    else {
        log.trace "updated(): Ran within last 2 seconds so aborting."
    }
}