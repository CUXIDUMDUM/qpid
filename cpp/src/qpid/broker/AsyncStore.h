/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#ifndef qpid_broker_AsyncStore_h_
#define qpid_broker_AsyncStore_h_

// TODO: See if we can replace this with a forward declaration, but current definition of qpid::types::Variant::Map
// does not allow it. Using a local map<std::string, Variant> definition also precludes forward declaration.
#include "qpid/types/Variant.h" // qpid::types::Variant::Map

#include <stdint.h>
#include <string>

namespace qpid {
namespace broker {

// Defined by broker, implements qpid::messaging::Handle-type template to hide ref counting
// Subclass this for specific contexts
class BrokerAsyncContext {
public:
    virtual ~BrokerAsyncContext();
};

// Callback definition:
//struct AsyncResult {
//    int errNo; // 0 implies no error
//    std::string errMsg;
//    AsyncResult();
//    AsyncResult(const int errNo,
//                const std::string& errMsg);
//    void destroy();
//};
//typedef void (*ResultCallback)(const AsyncResult*, BrokerAsyncContext*);

class AsyncResultHandle;
class AsyncResultQueue; // Implements the result callback function

// Singleton class in broker which contains return pollable queue. Use submitAsyncResult() to add reulsts to queue.
class AsyncResultHandler {
public:
    virtual ~AsyncResultHandler();

    // Factory method to create result handle

    virtual AsyncResultHandle createAsyncResultHandle(const int errNo, const std::string& errMsg, BrokerAsyncContext*) = 0;

    // Async return interface

    virtual void submitAsyncResult(AsyncResultHandle&) = 0;
};
typedef void (qpid::broker::AsyncResultQueue::*ResultCallback)(AsyncResultHandle*);
//typedef void (qpid::broker::AsyncResultQueue::*ResultCallback)(AsyncResultQueue*, AsyncResultHandle*);

class DataSource {
public:
    virtual ~DataSource();
    virtual uint64_t getSize() = 0;
    virtual void write(char* target) = 0;
};

class ConfigHandle;
class EnqueueHandle;
class EventHandle;
class MessageHandle;
class QueueHandle;
class TxnHandle;


// Subclassed by store:
class AsyncStore {
public:
    AsyncStore();
    virtual ~AsyncStore();

    // Factory methods for creating handles

    virtual ConfigHandle createConfigHandle() = 0;
    virtual EnqueueHandle createEnqueueHandle(MessageHandle&, QueueHandle&) = 0;
    virtual EventHandle createEventHandle(QueueHandle&, const std::string& key=std::string()) = 0;
    virtual MessageHandle createMessageHandle(const DataSource*) = 0;
    virtual QueueHandle createQueueHandle(const std::string& name, const qpid::types::Variant::Map& opts) = 0;
    virtual TxnHandle createTxnHandle(const std::string& xid=std::string()) = 0;


    // Store async interface

    virtual void submitPrepare(TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0; // Distributed txns only
    virtual void submitCommit(TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitAbort(TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0;

    virtual void submitCreate(ConfigHandle&, const DataSource*, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitDestroy(ConfigHandle&, ResultCallback, BrokerAsyncContext*) = 0;

    virtual void submitCreate(QueueHandle&, const DataSource*, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitDestroy(QueueHandle&, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitFlush(QueueHandle&, ResultCallback, BrokerAsyncContext*) = 0;

    virtual void submitCreate(EventHandle&, const DataSource*, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitCreate(EventHandle&, const DataSource*, TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitDestroy(EventHandle&, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitDestroy(EventHandle&, TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0;

    virtual void submitEnqueue(EnqueueHandle&, TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0;
    virtual void submitDequeue(EnqueueHandle&, TxnHandle&, ResultCallback, BrokerAsyncContext*) = 0;

    // Legacy - Restore FTD message, is NOT async!
    virtual int loadContent(MessageHandle&, QueueHandle&, char* data, uint64_t offset, const uint64_t length) = 0;
};


}} // namespace qpid::broker

#endif // qpid_broker_AsyncStore_h_
