// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

class TopicMetricCoolDevSGI8600CRC extends TopicBaseProcessor {
    TopicMetricCoolDevSGI8600CRC(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        processNumberKey("TempC1AirInBot", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC1AirInMid", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC1AirInTop", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC1AirOutBot", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC1AirOutMid", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC1AirOutTop", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempSysLiqOut", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC1LiqOut", "C", "TEMPERATURE", data, map, results);
        processNumberKey("ValveLiqC1FB", "%", "PERCENT_CLOSED", data, map, results);
        processNumberKey("ValveLiqC2FB", "%", "PERCENT_CLOSED", data, map, results);
        processNumberKey("ValveLiqC1", "%", "PERCENT_CLOSED", data, map, results);
        processNumberKey("ValveLiqC2", "%", "PERCENT_CLOSED", data, map, results);
        processNumberKey("BlowerBotOutput", "%", "PERCENT", data, map, results);
        processNumberKey("BlowerMidOutput", "%", "PERCENT", data, map, results);
        processNumberKey("BlowerTopOutput", "%", "PERCENT", data, map, results);
        processNumberKey("TempC2AirInBot", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC2AirInMid", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC2AirInTop", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC2AirOutBot", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC2AirOutMid", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC2AirOutTop", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempSysLiqIn", "C", "TEMPERATURE", data, map, results);
        processNumberKey("TempC2LiqOut", "C", "TEMPERATURE", data, map, results);
        processNumberKey("PressSysLiqOut", "PSI", "PRESSURE", data, map, results);
        processNumberKey("PressSysLiqIn", "PSI", "PRESSURE", data, map, results);
        processNumberKey("BlowerBotFB", "%", "PERCENT", data, map, results);
        processNumberKey("BlowerMidFB", "%", "PERCENT", data, map, results);
        processNumberKey("BlowerTopFB", "%", "PERCENT", data, map, results);
    }
}
