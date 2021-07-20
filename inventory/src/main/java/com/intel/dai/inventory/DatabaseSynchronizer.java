package com.intel.dai.inventory;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.HWInvUtil;
import com.intel.dai.dsapi.InventorySnapshot;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.api.database.RawInventoryDataIngester;
import com.intel.dai.inventory.api.es.Elasticsearch;
import com.intel.dai.inventory.api.es.ElasticsearchIndexIngester;
import com.intel.dai.inventory.api.es.RawNodeInventoryIngester;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Objects;

/**
 * Synchronizes synchronizes the inventory data in voltdb, postgres and foreign server.
 */
public class DatabaseSynchronizer {
    private final Logger log_;
    private final DataStoreFactory factory_;
    protected HWInvUtil util_;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    InventorySnapshot nearLineInventoryDatabaseClient_;                 // postgres
    RawNodeInventoryIngester rawNodeInventoryIngester;

    long totalNumberOfInjectedDocuments = 0;                            // for testing only
    ImmutablePair<Long, String> characteristicsOfLastRawDimmIngested;
    ImmutablePair<Long, String> characteristicsOfLastRawFruHostIngested;

    final long dataMoverTimeOutLimit = 60 * 1000;    // wait at most 1 minute

    private String hostName_ = "localhost";
    private int port_ = 9200;
    private String userName_ = "";
    private String password_ = "";

    public DatabaseSynchronizer(Logger log, NetworkListenerConfig config) {
        log_ = log;

        factory_ = ProviderInventoryNetworkForeignBus.getDataStoreFactory();
        if (factory_ == null) {
            log_.error("ProviderInventoryNetworkForeignBus.getDataStoreFactory() => null");
            return;
        }
        rawNodeInventoryIngester = new RawNodeInventoryIngester(factory_, log_);

        PropertyMap configMap = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if (configMap != null) {
            try {
                hostName_ = configMap.getString("hostName");
                port_ = configMap.getInt("port");
                userName_ = configMap.getString("userName");
                password_ = configMap.getString("password");
            } catch (PropertyNotExpectedType propertyNotExpectedType) {
                log_.error(propertyNotExpectedType.getMessage());
            }
            return;
        }
        log_.error("getProviderConfigurationFromClassName(%s) => null", getClass().getCanonicalName());
    }

    // For testing only
    void setElasticsearchServerAttributes(String hostName, int port, String userName, String password) {
        hostName_ = hostName;
        port_ = port;
        userName_ = userName;
        password_ = password;
    }

    void updateDaiInventoryTables() {
        try {
            initializeDependencies();

            log_.info("hostName:%s port:%s userName:%s password:%s", hostName_, port_, userName_, password_);
            if (areEmptyInventoryTablesInPostgres()) {
                log_.info("areEmptyInventoryTablesInPostgres() => true");
                Elasticsearch es = new Elasticsearch(log_);
                RestHighLevelClient esClient = es.getRestHighLevelClient(hostName_, port_,
                        userName_, password_);

                characteristicsOfLastRawDimmIngested = ingest(esClient, "kafka_dimm");
                characteristicsOfLastRawFruHostIngested = ingest(esClient, "kafka_fru_host");

                es.close();

                sleepForOneSecond();
                waitForDataMoverToFinish();

                rawNodeInventoryIngester.ingestInitialNodeInventoryHistory();
                totalNumberOfInjectedDocuments += rawNodeInventoryIngester.getNumberNodeInventoryJsonIngested();
                log_.info("Number of Raw_Node_Inventory_History documents = %d", rawNodeInventoryIngester.getNumberNodeInventoryJsonIngested());
                return;
            }
            log_.info("areEmptyInventoryTablesInPostgres() => false");
        } catch (DataStoreException e) {
            log_.error(e.getMessage());
        } finally {
            log_.info("updateDaiInventoryTables() completed");
        }
    }

    void ingestRawDimm(ImmutablePair<String, String> doc) {
        RawInventoryDataIngester.ingestDimm(doc);
        RawInventoryDataIngester.waitForRawDimmToAppearInNearLine(doc);
    }

    void ingestRawFruHost(ImmutablePair<String, String> doc) {
        RawInventoryDataIngester.ingestFruHost(doc);
        RawInventoryDataIngester.waitForRawFruHostToAppearInNearLine(doc);
    }

    void constructAndIngestNodeInventoryHistory(Dimm dimm) throws DataStoreException {
        rawNodeInventoryIngester.constructAndIngestNodeInventoryJson(dimm);
    }

    void constructAndIngestNodeInventoryHistory(FruHost fruHost) throws DataStoreException {
        rawNodeInventoryIngester.constructAndIngestNodeInventoryJson(fruHost);
    }

    private ImmutablePair<Long, String> ingest(RestHighLevelClient esClient, String index) throws DataStoreException {
        ElasticsearchIndexIngester eii = new ElasticsearchIndexIngester(esClient, index, 0, factory_, log_);
        eii.ingestIndexIntoVoltdb();
        totalNumberOfInjectedDocuments += eii.getNumberOfDocumentsEnumerated();
        log_.info("Number of %s documents = %d", index, eii.getNumberOfDocumentsEnumerated());
        return eii.getCharacteristicsOfLastDocIngested();
    }

    private void waitForDataMoverToFinish() {
        waitForDataMoverToFinishMovingRawDimms();
        waitForDataMoverToFinishMovingRawFruHosts();
    }

    private void waitForDataMoverToFinishMovingRawDimms() {
        if (characteristicsOfLastRawDimmIngested.right == null) {
            log_.error("characteristicsOfLastRawDimmIngested.right == null");
            return;
        }

        long timeOut = dataMoverTimeOutLimit;
        while (timeOut > 0) {
            ImmutablePair<Long, String> lastRawDimmTransferred = getCharacteristicsOfLastRawDimmIngestedIntoNearLine();
            if (lastRawDimmTransferred.right != null) {
                log_.info("Comparing characteristicsOfLastRawDimmIngested <%d, %s> against lastRawDimmTransferred <%d, %s>",
                        characteristicsOfLastRawDimmIngested.left, characteristicsOfLastRawDimmIngested.right,
                        lastRawDimmTransferred.left, lastRawDimmTransferred.right);
                if (characteristicsOfLastRawDimmIngested.equals(lastRawDimmTransferred)) {
                    log_.info("Raw DIMMs transfer completed at %dms", dataMoverTimeOutLimit - timeOut);
                    return;
                }
            } else {
                log_.error("lastRawDimmTransferred.right == null");
            }
            log_.info("Waiting for data mover - Raw DIMMs: %dms left", timeOut);
            timeOut -= sleepForOneSecond();
        }
    }

    private void waitForDataMoverToFinishMovingRawFruHosts() {
        if (characteristicsOfLastRawFruHostIngested.right == null) {
            log_.error("characteristicsOfLastRawFruHostIngested.right == null");
            return;
        }

        long timeOut = dataMoverTimeOutLimit;
        while (timeOut > 0) {
            ImmutablePair<Long, String> lastRawFruHostTransferred = getCharacteristicsOfLastRawFruHostIngestedIntoNearLine();
            if (lastRawFruHostTransferred.right != null) {
                log_.info("Comparing characteristicsOfLastRawFruHostIngested <%d, %s> against lastRawFruHostTransferred <%d, %s>",
                        characteristicsOfLastRawFruHostIngested.left, characteristicsOfLastRawFruHostIngested.right,
                        lastRawFruHostTransferred.left, lastRawFruHostTransferred.right);
                if (characteristicsOfLastRawFruHostIngested.equals(lastRawFruHostTransferred)) {
                    log_.info("Raw FRU Hosts transfer completed at %dms", dataMoverTimeOutLimit - timeOut);
                    return;
                }
            } else {
                log_.error("lastRawFruHostTransferred.right == null");
            }
            log_.info("Waiting for data mover - Raw FRU Hosts: %dms left", timeOut);
            timeOut -= sleepForOneSecond();
        }
    }

    /**
     * InterruptedExceptions are ignored.
     * @return sleepLength
     */
    private long sleepForOneSecond() {
        final long sleepLengthWaitingForDataMoverToFinish = 1000;
        try {
            Thread.sleep(sleepLengthWaitingForDataMoverToFinish);
        } catch (InterruptedException e) {
            log_.info(e.getMessage());
        }
        return sleepLengthWaitingForDataMoverToFinish;
    }

    private void initializeDependencies() {
        util_ = new HWInvUtilImpl(log_);

        onlineInventoryDatabaseClient_ = factory_.createHWInvApi();
        nearLineInventoryDatabaseClient_ = factory_.createInventorySnapshotApi();
    }

    boolean areEmptyInventoryTablesInPostgres() {
        ImmutablePair<Long, String> lastRawDimm =
                nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawDimmIngested();
        log_.info("lastRawDimm: %d %s", lastRawDimm.left, lastRawDimm.right);
        ImmutablePair<Long, String> lastRawFruHost =
                nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawFruHostIngested();
        log_.info("lastRawFruHost: %d %s", lastRawFruHost.left, lastRawFruHost.right);

        return lastRawDimm.right == null && lastRawFruHost.right == null;
    }

    ImmutablePair<Long, String> getCharacteristicsOfLastRawDimmIngestedIntoNearLine() {
        log_.info(">> getCharacteristicsOfLastRawDimmIngestedIntoNearLine()");
        try {
            ImmutablePair<Long, String> lastIngestedRawDimm =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawDimmIngested();
            return Objects.requireNonNullElse(lastIngestedRawDimm, ImmutablePair.nullPair());
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
            return ImmutablePair.nullPair();
        }
    }

    ImmutablePair<Long, String> getCharacteristicsOfLastRawFruHostIngestedIntoNearLine() {  // must not be private or Spy will not work
        log_.info(">> getCharacteristicsOfLastRawFruHost()");
        try {
            ImmutablePair<Long, String> lastIngestedRawFruHost =
                    nearLineInventoryDatabaseClient_.getCharacteristicsOfLastRawFruHostIngested();
            return Objects.requireNonNullElse(lastIngestedRawFruHost, ImmutablePair.nullPair());
        } catch (NullPointerException e) {
            log_.exception(e, "null pointer exception: %s", e.getMessage());
            return ImmutablePair.nullPair();
        }
    }
}
