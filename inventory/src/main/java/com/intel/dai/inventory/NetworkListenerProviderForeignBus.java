// Copyright (C) 2019-2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.google.gson.Gson;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of class BootEventTransformer.
 */
public class NetworkListenerProviderForeignBus implements NetworkListenerProvider, Initializer {
    public NetworkListenerProviderForeignBus(Logger logger) {
        log_ = logger;
    }

    @Override
    public void initialize() { /* Not used but is required */ }

    /**
     * <p> Callback from com.intel.dai.network_listener.processMessage().
     * Translates the json in the data String into CommonDataFormat.  The json contains a list of components
     * that shares a common state update.  This translate into an array of CommonDataFormat entries, each
     * describing a single component. </p>
     *
     * @param topic The topic or topic associated with this message.
     * @param inventoryJson state change notification as a json string
     * @param config network listener configuration
     * @return always return an empty List<CommonDataFormat> because no postprocessing is necessary
     */
    @Override
    public List<CommonDataFormat> processRawStringData(String topic, String inventoryJson, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        log_.debug("Kafka data received %s: %s", topic, inventoryJson);

        DatabaseSynchronizer synchronizer = new DatabaseSynchronizer(log_, config);
        switch (topic) {
            case "kafka_dimm":
                synchronizer.ingestRawDimm(new ImmutablePair<>(topic, inventoryJson));
                try {
                    Dimm dimm = gson.fromJson(inventoryJson, Dimm.class);
                    synchronizer.constructAndIngestNodeInventoryHistory(dimm);
                } catch (DataStoreException e) {
                    throw new NetworkListenerProviderException(e.getMessage());
                }
                break;
            case "kafka_fru_host":
                synchronizer.ingestRawFruHost(new ImmutablePair<>(topic, inventoryJson));
                try {
                    FruHost fruHost = gson.fromJson(inventoryJson, FruHost.class);
                    synchronizer.constructAndIngestNodeInventoryHistory(fruHost);
                } catch (DataStoreException e) {
                    throw new NetworkListenerProviderException(e.getMessage());
                }
                break;
            default:
                log_.error("Unexpected kafka topic: %s", topic);
                break;
        }

        return new ArrayList<>();   // to be consumed by com.intel.dai.network_listener.processMessage()
    }

    /**
     * <p> Callback from com.intel.dai.network_listener.processMessage().
     * Acts on a work item described in common data format. </p>
     * @param workItem HW inventory update work item in common data format
     * @param config network listener config
     * @param systemActions system actions for processing work item
     */
    @Override
    public void actOnData(CommonDataFormat workItem, NetworkListenerConfig config, SystemActions systemActions) {
    }

    private final Logger log_;
    private final static Gson gson = new Gson();
}
