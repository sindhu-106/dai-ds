// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import java.util.BitSet;
import static java.lang.Math.toIntExact;
import org.voltdb.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handle the database processing that is necessary to queue a work item in the WorkItem "queue".
 * NOTE:
 *
 *  Returns: long lThisWorkItemId = The work item id for this work item that is being "queued"
 *
 *  Input parameter:
 *     String sQueue              = The "queue" this item belongs to
 *     String sWorkingAdapterType = The type of adapter that should handle this item
 *     String sWorkToBeDone       = What specific work should this item do (e.g., SessionAllocate, JobStarting, JobRunning, JobTerminated, SessionFree)
 *     String sParms              = Parameters for this work item
 *     String sNotifyWhenFinished = Flag indicating whether requester only wants the initial ack indicating the item was successfully queued
 *                                  T = Wants the normal default processing flow (all acks will be returned)
 *                                  F = Shortcut processing, don't notify requester when work item is finished
 *     String sReqAdapterType     = What type of adapter requested this new work item (aka sender)
 *     long   lReqWorkItemId      = Unique id that identifies which Work Item requested/started this new work item (aka sender/requester) (e.g. 40000000000)
 *                                  4 is the prefix for WLM
 *                                  0000000000 is the next sequence number for items generated by WLM
 *
 *  Sample invocation:
 *      echo "Exec WorkItemQueue Session, ONLINE_TIER, SessionAllocate, ParmsGoHere, T, WLM, 40000000000;" | sqlcmd
 *          Queue a new work item
 *              Ensure that the adapter type is a valid choice
 *              Generate a unique work item id for this new work item
 *              Insert a row in the WorkItem table for this new item
 *                  Set Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, and NotifyWhenFinished from specified values
 *                  Set Id from the generated unique adapter id
 *                  Set State to Queued/Requested
 *                  Set StartTimestamp and LastChgTimestamp with the current date time
 */

public class WorkItemQueue extends VoltProcedure {

    public final SQLStmt selectUniqueIdSql = new SQLStmt("SELECT NextValue FROM UniqueValues WHERE Entity = ? Order By Entity;");
    public final SQLStmt updateUniqueIdSql = new SQLStmt("UPDATE UniqueValues SET NextValue = NextValue + 1, DbUpdatedTimestamp = ? WHERE Entity = ?;");
    public final SQLStmt insertUniqueIdSql = new SQLStmt("INSERT INTO UniqueValues (Entity, NextValue, DbUpdatedTimestamp) VALUES (?, ?, ?);");


    public final SQLStmt insertWorkItemSql = new SQLStmt(
            "INSERT INTO WorkItem " +
            "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, StartTimestamp, DbUpdatedTimestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'Q', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);"
    );

    public final SQLStmt insertWorkItemHistorySql = new SQLStmt(
            "INSERT INTO WorkItem_History " +
            "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, StartTimestamp, DbUpdatedTimestamp, RowInsertedIntoHistory) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'Q', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'T');"
    );


    public long run(String sQueue, String sWorkingAdapterType, String sWorkToBeDone, String sParms, String sNotifyWhenFinished, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {

        //---------------------------------------------------------------------
        // Ensure that the specified adapter type is a valid adapter type
        // - when adding a new adapter type also add to the AdapterStarted.java stored procedure.
        //---------------------------------------------------------------------
        switch(sWorkingAdapterType.toUpperCase()) {
            case "WLM":
            case "PROVISIONER":
            case "RAS":
            case "ONLINE_TIER":
            case "NEARLINE_TIER":
            case "MONITOR":
            case "RM_RTE":
            case "FM":
            case "UI":
            case "CONTROL":
            case "DIAGNOSTICS":
            case "DAI_MGR":
            case "SERVICE":
            case "INVENTORY":
            case "POWER_MANAGER":
            case "INITIALIZATION":
            case "ALERT_MGR":
                break;
            default:
                throw new VoltAbortException("WorkItemQueue - can't queue work item (Queue=" + sQueue + ", WorkingAdapterType=" + sWorkingAdapterType + ", WorkToBeDone=" + sWorkToBeDone + "), " +
                                             "because an invalid WorkingAdapterType was specified (" + sWorkingAdapterType + ")!");
        }

        //---------------------------------------------------------------------
        // Ensure that the specified flag for sNotifyWhenFinished is either a 'T' or 'F'
        //---------------------------------------------------------------------
        if ((!sNotifyWhenFinished.equals("T") && (!sNotifyWhenFinished.equals("F")))) {
            throw new VoltAbortException("WorkItemQueue - can't queue work item (Queue=" + sQueue + ", WorkingAdapterType=" + sWorkingAdapterType + ", WorkToBeDone=" + sWorkToBeDone + "), " +
                                         "because an invalid value was specified for sNotifyWhenFinished (" + sNotifyWhenFinished + ")!");
        }

        //--------------------------------------------------
        // Generate a unique id for this new work item.
        //--------------------------------------------------
        final String Entity = sWorkingAdapterType.toUpperCase();
        // Get the current "next unique id" for the specified entity.
        voltQueueSQL(selectUniqueIdSql, EXPECT_ZERO_OR_ONE_ROW, Entity);
        VoltTable[] uniqueId = voltExecuteSQL();
        // Check and see if there is a matching record for the specified entity
        if (uniqueId[0].getRowCount() == 0) {
            // No matching record for the specified entity - add a new row for the specified entity
            voltQueueSQL(insertUniqueIdSql, Entity, 1, this.getTransactionTime());
            voltExecuteSQL();
            // Now redo the above query (to get the current "next unique id" for the specified entity)
            voltQueueSQL(selectUniqueIdSql, EXPECT_ONE_ROW, Entity);
            uniqueId = voltExecuteSQL();
        }
        // Save away the generated unique adapter id.
        long lThisWorkItemId = uniqueId[0].asScalarLong();
        // Bump the current "next unique id" to generate the next "next unique id" for the specified entity.
        voltQueueSQL(updateUniqueIdSql, EXPECT_ONE_ROW, this.getTransactionTime(), Entity);
        voltExecuteSQL();

        //---------------------------------------------------------------------
        // Enqueue this work item (insert a new row into the WorkItem table)
        //---------------------------------------------------------------------
        voltQueueSQL(insertWorkItemSql, sQueue, sWorkingAdapterType.toUpperCase(), lThisWorkItemId, sWorkToBeDone, sParms, sNotifyWhenFinished, lReqWorkItemId, sReqAdapterType);

        //---------------------------------------------------------------------
        // Also insert a copy of this information into the History table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertWorkItemHistorySql, sQueue, sWorkingAdapterType.toUpperCase(), lThisWorkItemId, sWorkToBeDone, sParms, sNotifyWhenFinished, lReqWorkItemId, sReqAdapterType);
        voltExecuteSQL(true);

        // Return this new work item's id to the caller.
        return lThisWorkItemId;
    }
}