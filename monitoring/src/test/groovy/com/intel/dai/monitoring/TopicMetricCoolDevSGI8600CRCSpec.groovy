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

class TopicMetricCoolDevSGI8600CRCSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricCoolDevSGI8600CRC(Mock(Logger), false)
    }

    def "Test processTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
            PropertyMap map = new PropertyMap()
            map.put("TempC1AirInBot", 10.0)
            map.put("TempC1AirOutTop", 10.0)
            map.put("ValveLiqC1FB", 40.0)
            if(!SKIP)
                map.put("BlowerBotOutput", VALUE)
            map.put("TempC2AirOutBot", 45.0)
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
