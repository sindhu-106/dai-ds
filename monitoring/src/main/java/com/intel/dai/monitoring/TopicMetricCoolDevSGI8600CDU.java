// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

class TopicMetricCoolDevSGI8600CDU extends TopicBaseProcessor {
    TopicMetricCoolDevSGI8600CDU(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        processNumberKeyWithFactor("SGI_CDU_PS3", "PSI", "PRESSURE", data, map, results, 14.5038);
        processNumberKey("SGI_CDU_Secondary_Temperature_T2a", "C", "TEMPERATURE", data, map, results);
        processNumberKeyWithFactor("SGI_CDU_Primary_Flow_Rate", "GPM", "FLOW_RATE", data, map, results, 0.26417);
        processNumberKey("SGI_CDU_Dew_Point", "C", "TEMPERATURE", data, map, results);
        processNumberKey("SGI_CDU_Secondary_Temperature_T2b", "C", "TEMPERATURE", data, map, results);
        processNumberKey("SGI_CDU_Sec_Return_Temperature_T4", "C", "TEMPERATURE", data, map, results);
        processNumberKey("SGI_CDU_Secondary_Temperature_T2", "C", "TEMPERATURE", data, map, results);
        processNumberKey("SGI_CDU_Current_SetPoint", "C", "TEMPERATURE", data, map, results);
        processNumberKeyWithFactor("SGI_CDU_Sec_Flow_Rate", "GPM", "FLOW_RATE", data, map, results, 0.26417);
        processNumberKeyWithFactor("SGI_CDU_Unit_Duty", "W", "POWER", data, map, results, 1_000.0);
        processNumberKeyWithFactor("SGI_CDU_Sec_Diff_Press", "PSI", "PRESSURE_DELTA", data, map, results, 14.5038);
        processNumberKey("SGI_CDU_Sec_SetPoint", "C", "TEMPERATURE", data, map, results);
        processNumberKeyWithFactor("SGI_CDU_PS4", "PSI", "PRESSURE", data, map, results, 14.5038);
        processNumberKeyWithFactor("SGI_CDU_Flow_SetPoint", "GPM", "FLOW_RATE", data, map, results, 0.26417);
        processNumberKey("SGI_CDU_Room_Temperature_T3", "C", "TEMPERATURE", data, map, results);
        processNumberKey("SGI_CDU_Room_Relative_Humidity", "%", "HUMIDITY", data, map, results);
        processNumberKeyWithFactor("SGI_CDU_PS2", "PSI", "PRESSURE", data, map, results, 14.5038);
        processNumberKey("SGI_CDU_Primary_Temperature_T1", "C", "TEMPERATURE", data, map, results);
        processNumberKeyWithFactor("SGI_CDU_PS1", "PSI", "PRESSURE", data, map, results, 14.5038);
        processNumberKey("SGI_CDU_Pump_Speed", "", "SPEED", data, map, results);
        processNumberKeyWithFactor("SGI_CDU_Filter_Pressure_Difference", "PSI", "PRESSURE_DELTA", data, map, results, 14.5038);
    }
}
