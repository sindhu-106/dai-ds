// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory


import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.logging.Logger
import spock.lang.Specification

class DatabaseSynchronizerSpec extends Specification {
    def ts = new DatabaseSynchronizer(Mock(Logger),
//            Mock(DataStoreFactory),
            Mock(NetworkListenerConfig))

//    def "initializeDependencies"() {
//        when: ts.initializeDependencies()
//        then:
//        ts.util_ != null
//        ts.foreignInventoryDatabaseClient_ != null
//    }
}
