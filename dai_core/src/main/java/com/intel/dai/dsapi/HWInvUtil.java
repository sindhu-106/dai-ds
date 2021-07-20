// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * This interface contains methods that convert canonical inventory information amongst different formats.  The
 * possible formats are POJO, json string and json file.
 */
public interface HWInvUtil {
    HWInvHistory toCanonicalHistoryPOJO(String canonicalHWInvHistoryJson);
    String toCanonicalHistoryJson(HWInvHistory history);
    void toFile(String str, String outputFileName) throws IOException;
    String fromFile(Path inputFilePath) throws IOException;
    String head(String str, int limit);
    void setRemainingNumberOfErrorMessages(int limit);
    void setRemainingNumberOfInfoMessages(int limit);
    void logError(String fmt, Object ...args);
    void logInfo(String fmt, Object ...args);
}
