/*
 *
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
 *
 */

require(["dojo/store/JsonRest",
         "dojo/json",
         "dojo/query",
         "dojo/_base/xhr",
         "dojo/dom",
         "qpid/common/formatter",
         "qpid/common/updater",
         "qpid/common/UpdatableStore",
         "dojo/domReady!"],
	    function(JsonRest, json, query, xhr, dom, formatter, updater, UpdatableStore)
	    {

         function QueueUpdater()
         {
            this.name = dom.byId("name");
            this.state = dom.byId("state");
            this.durable = dom.byId("durable");
            this.lifetimePolicy = dom.byId("lifetimePolicy");
            this.queueDepthMessages = dom.byId("queueDepthMessages");
            this.queueDepthBytes = dom.byId("queueDepthBytes");
            this.queueDepthBytesUnits = dom.byId("queueDepthBytesUnits");
            this.unacknowledgedMessages = dom.byId("unacknowledgedMessages");
            this.unacknowledgedBytes = dom.byId("unacknowledgedBytes");
            this.unacknowledgedBytesUnits = dom.byId("unacknowledgedBytesUnits");

            var urlQuery = dojo.queryToObject(dojo.doc.location.search.substr((dojo.doc.location.search[0] === "?" ? 1 : 0)));
            this.query = "/rest/queue/"+ urlQuery.vhost + "/" + urlQuery.queue;


            var that = this;

            xhr.get({url: this.query, sync: useSyncGet, handleAs: "json"}).then(function(data)
                             {
                                that.queueData = data[0];

                                flattenStatistics( that.queueData );

                                that.updateHeader();
                                that.bindingsGrid = new UpdatableStore(that.queueData.bindings, "bindings",
                                                         [ { name: "Exchange",    field: "exchange",      width: "90px"},
                                                           { name: "Binding Key", field: "name",          width: "120px"},
                                                           { name: "Arguments",   field: "argumentString",     width: "100%"}
                                                         ]);

                                that.consumersGrid = new UpdatableStore(that.queueData.consumers, "consumers",
                                                         [ { name: "Name",    field: "name",      width: "70px"},
                                                           { name: "Mode", field: "distributionMode", width: "70px"},
                                                           { name: "Msgs Rate", field: "msgRate",
                                                           width: "150px"},
                                                           { name: "Bytes Rate", field: "bytesRate",
                                                              width: "100%"}
                                                         ]);



                             });

         }

         QueueUpdater.prototype.updateHeader = function()
         {
             var bytesDepth;
            this.name.innerHTML = this.queueData[ "name" ];
            this.state.innerHTML = this.queueData[ "state" ];
            this.durable.innerHTML = this.queueData[ "durable" ];
            this.lifetimePolicy.innerHTML = this.queueData[ "lifetimePolicy" ];

            this.queueDepthMessages.innerHTML = this.queueData["queueDepthMessages"];
            bytesDepth = formatter.formatBytes( this.queueData["queueDepthBytes"] );
            this.queueDepthBytes.innerHTML = "(" + bytesDepth.value;
            this.queueDepthBytesUnits.innerHTML = bytesDepth.units + ")";

            this.unacknowledgedMessages.innerHTML = this.queueData["unacknowledgedMessages"];
            bytesDepth = formatter.formatBytes( this.queueData["unacknowledgedBytes"] );
            this.unacknowledgedBytes.innerHTML = "(" + bytesDepth.value;
            this.unacknowledgedBytesUnits.innerHTML = bytesDepth.units + ")"

         };

         QueueUpdater.prototype.update = function()
         {

            var thisObj = this;

            xhr.get({url: this.query, sync: useSyncGet, handleAs: "json"}).then(function(data)
                 {
                     var i,j;
                    thisObj.queueData = data[0];
                    flattenStatistics( thisObj.queueData );

                    var bindings = thisObj.queueData[ "bindings" ];
                    var consumers = thisObj.queueData[ "consumers" ];

                    for(i=0; i < bindings.length; i++)
                    {
                        bindings[i].argumentString = json.stringify(bindings[i].arguments);
                    }

                    thisObj.updateHeader();


                    // update alerting info
                    var alertRepeatGap = formatter.formatTime( thisObj.queueData["alertRepeatGap"] );

                    dom.byId("alertRepeatGap").innerHTML = alertRepeatGap.value;
                    dom.byId("alertRepeatGapUnits").innerHTML = alertRepeatGap.units;


                    var alertMsgAge = formatter.formatTime( thisObj.queueData["alertThresholdMessageAge"] );

                    dom.byId("alertThresholdMessageAge").innerHTML = alertMsgAge.value;
                    dom.byId("alertThresholdMessageAgeUnits").innerHTML = alertMsgAge.units;

                    var alertMsgSize = formatter.formatBytes( thisObj.queueData["alertThresholdMessageSize"] );

                    dom.byId("alertThresholdMessageSize").innerHTML = alertMsgSize.value;
                    dom.byId("alertThresholdMessageSizeUnits").innerHTML = alertMsgSize.units;

                    var alertQueueDepth = formatter.formatBytes( thisObj.queueData["alertThresholdQueueDepthBytes"] );

                    dom.byId("alertThresholdQueueDepthBytes").innerHTML = alertQueueDepth.value;
                    dom.byId("alertThresholdQueueDepthBytesUnits").innerHTML = alertQueueDepth.units;

                    dom.byId("alertThresholdQueueDepthMessages").innerHTML = thisObj.queueData["alertThresholdQueueDepthMessages"];

                    var sampleTime = new Date();
                    var messageIn = thisObj.queueData["totalEnqueuedMessages"];
                    var bytesIn = thisObj.queueData["totalEnqueuedBytes"];
                    var messageOut = thisObj.queueData["totalDequeuedMessages"];
                    var bytesOut = thisObj.queueData["totalDequeuedBytes"];

                    if(thisObj.sampleTime)
                    {
                        var samplePeriod = sampleTime.getTime() - thisObj.sampleTime.getTime();

                        var msgInRate = (1000 * (messageIn - thisObj.messageIn)) / samplePeriod;
                        var msgOutRate = (1000 * (messageOut - thisObj.messageOut)) / samplePeriod;
                        var bytesInRate = (1000 * (bytesIn - thisObj.bytesIn)) / samplePeriod;
                        var bytesOutRate = (1000 * (bytesOut - thisObj.bytesOut)) / samplePeriod;

                        dom.byId("msgInRate").innerHTML = msgInRate.toFixed(0);
                        var bytesInFormat = formatter.formatBytes( bytesInRate );
                        dom.byId("bytesInRate").innerHTML = "(" + bytesInFormat.value;
                        dom.byId("bytesInRateUnits").innerHTML = bytesInFormat.units + "/s)";

                        dom.byId("msgOutRate").innerHTML = msgOutRate.toFixed(0);
                        var bytesOutFormat = formatter.formatBytes( bytesOutRate );
                        dom.byId("bytesOutRate").innerHTML = "(" + bytesOutFormat.value;
                        dom.byId("bytesOutRateUnits").innerHTML = bytesOutFormat.units + "/s)";

                        if(consumers && thisObj.consumers)
                        {
                            for(i=0; i < consumers.length; i++)
                            {
                                var consumer = consumers[i];
                                for(j = 0; j < thisObj.consumers.length; j++)
                                {
                                    var oldConsumer = thisObj.consumers[j];
                                    if(oldConsumer.id == consumer.id)
                                    {
                                        var msgRate = (1000 * (consumer.messagesOut - oldConsumer.messagesOut)) /
                                                        samplePeriod;
                                        consumer.msgRate = msgRate.toFixed(0) + "msg/s";

                                        var bytesRate = (1000 * (consumer.bytesOut - oldConsumer.bytesOut)) /
                                                        samplePeriod;
                                        var bytesRateFormat = formatter.formatBytes( bytesRate );
                                        consumer.bytesRate = bytesRateFormat.value + bytesRateFormat.units + "/s";
                                    }


                                }

                            }
                        }

                    }

                    thisObj.sampleTime = sampleTime;
                    thisObj.messageIn = messageIn;
                    thisObj.bytesIn = bytesIn;
                    thisObj.messageOut = messageOut;
                    thisObj.bytesOut = bytesOut;
                    thisObj.consumers = consumers;

                    // update bindings
                    thisObj.bindingsGrid.update(thisObj.queueData.bindings);

                    // update consumers
                    thisObj.consumersGrid.update(thisObj.queueData.consumers)


                 });
         };

         var queueUpdater = new QueueUpdater();

         updater.add( queueUpdater );

         queueUpdater.update();

     });


require([
    "dojo/store/JsonRest",
    "dojox/grid/EnhancedGrid",
    "dojo/data/ObjectStore",
    "dojox/grid/enhanced/plugins/Pagination",
    "dojo/domReady!"
], function(JsonRest, EnhancedGrid, ObjectStore) {
    var urlQuery = dojo.queryToObject(dojo.doc.location.search.substr((dojo.doc.location.search[0] === "?" ? 1 : 0)));
    var myStore = new JsonRest({target:"/rest/message/"+ urlQuery.vhost + "/" + urlQuery.queue});
    var grid = new EnhancedGrid({
        store: dataStore = ObjectStore({objectStore: myStore}),
        structure: [
            {name:"Id", field:"id", width: "50px"},
            {name:"Size", field:"size", width: "60px"},
            {name:"State", field:"state", width: "120px"},
            {name:"Arrival", field:"arrivalTime", width: "100%"}
        ],
        plugins: {
                  pagination: {
                      pageSizes: ["10", "25", "50", "100"],
                      description: true,
                      sizeSwitch: true,
                      pageStepper: true,
                      gotoButton: true,
                      maxPageStep: 4,
                              /*position of the pagination bar*/
                      position: "bottom"
                  }
        }
    }, "messages");

    grid.startup();
});