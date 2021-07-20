package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.HWInvHistory
import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import org.voltdb.ClientResponseImpl
import org.voltdb.VoltTable
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification
import java.nio.file.Paths

class VoltHWInvDbApiSpec extends Specification {
    VoltHWInvDbApi api
    Logger logger = Mock(Logger)

    def setup() {
        VoltDbClient.voltClient = Mock(Client)
        String[] servers = ["localhost"]
        api = new VoltHWInvDbApi(logger, new HWInvUtilImpl(Mock(Logger)), servers)
    }

    def "initialize"() {
        when: api.initialize()
        then: notThrown Exception
    }
}
