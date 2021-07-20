// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import com.intel.runtime_utils.TimeUtils
import spock.lang.Specification

class TopicMetricCoolDevSGI8600CDUSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricCoolDevSGI8600CDU(Mock(Logger), false)
    }

    def "Test processTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
            PropertyMap map = new PropertyMap()
            map.put("SGI_CDU_PS3", 10.0)
            map.put("SGI_CDU_Primary_Flow_Rate", 10.0)
            map.put("SGI_CDU_Primary_Temperature_T1", 40.0)
            if(!SKIP)
                map.put("SGI_CDU_Flow_SetPoint", VALUE)
            map.put("SGI_CDU_Secondary_Temperature_T2", 45.0)
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == RESULT
        where:
            VALUE | SKIP  || RESULT
            10.0  | false || 5
            null  | false || 4
            10.0  | true  || 4
    }
}
