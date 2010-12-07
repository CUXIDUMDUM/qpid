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
package org.apache.qpid.server.queue;

import org.apache.qpid.AMQException;
import org.apache.qpid.exchange.ExchangeDefaults;
import org.apache.qpid.framing.AMQShortString;
import org.apache.qpid.framing.FieldTable;
import org.apache.qpid.server.configuration.QueueConfiguration;
import org.apache.qpid.server.configuration.ServerConfiguration;
import org.apache.qpid.server.exchange.Exchange;
import org.apache.qpid.server.exchange.ExchangeFactory;
import org.apache.qpid.server.exchange.ExchangeRegistry;
import org.apache.qpid.server.registry.ApplicationRegistry;
import org.apache.qpid.server.virtualhost.VirtualHost;

public class AMQQueueFactory
{
    public static final AMQShortString DLQ_ROUTING_KEY = new AMQShortString("dlq");
    public static final AMQShortString X_QPID_DLQ_ENABLED = new AMQShortString("x-qpid-dlq-enabled");
    public static final String DEFAULT_DLQ_NAME_SUFFIX = "_DLQ";
    public static final AMQShortString X_QPID_PRIORITIES = new AMQShortString("x-qpid-priorities");
    private static final AMQShortString QPID_LAST_VALUE_QUEUE = new AMQShortString ("qpid.last_value_queue");
    private static final AMQShortString QPID_LAST_VALUE_QUEUE_KEY = new AMQShortString("qpid.last_value_queue_key");
    private static final String QPID_LVQ_KEY = "qpid.LVQ_key";

    private abstract static class QueueProperty
    {

        private final AMQShortString _argumentName;


        public QueueProperty(String argumentName)
        {
            _argumentName = new AMQShortString(argumentName);
        }

        public AMQShortString getArgumentName()
        {
            return _argumentName;
        }


        public abstract void setPropertyValue(AMQQueue queue, Object value);

    }

    private abstract static class QueueLongProperty extends QueueProperty
    {

        public QueueLongProperty(String argumentName)
        {
            super(argumentName);
        }

        public void setPropertyValue(AMQQueue queue, Object value)
        {
            if(value instanceof Number)
            {
                setPropertyValue(queue, ((Number)value).longValue());
            }

        }

        abstract void setPropertyValue(AMQQueue queue, long value);


    }

    private static final QueueProperty[] DECLAREABLE_PROPERTIES = {
            new QueueLongProperty("x-qpid-maximum-message-age")
            {
                public void setPropertyValue(AMQQueue queue, long value)
                {
                    queue.setMaximumMessageAge(value);
                }
            },
            new QueueLongProperty("x-qpid-maximum-message-size")
            {
                public void setPropertyValue(AMQQueue queue, long value)
                {
                    queue.setMaximumMessageSize(value);
                }
            },
            new QueueLongProperty("x-qpid-maximum-message-count")
            {
                public void setPropertyValue(AMQQueue queue, long value)
                {
                    queue.setMaximumMessageCount(value);
                }
            },
            new QueueLongProperty("x-qpid-minimum-alert-repeat-gap")
            {
                public void setPropertyValue(AMQQueue queue, long value)
                {
                    queue.setMinimumAlertRepeatGap(value);
                }
            },
            new QueueLongProperty("x-qpid-capacity")
            {
                public void setPropertyValue(AMQQueue queue, long value)
                {
                    queue.setCapacity(value);
                }
            },
            new QueueLongProperty("x-qpid-flow-resume-capacity")
            {
                public void setPropertyValue(AMQQueue queue, long value)
                {
                    queue.setFlowResumeCapacity(value);
                }
            }

    };



    public static AMQQueue createAMQQueueImpl(AMQShortString name,
                                              boolean durable,
                                              AMQShortString owner,
                                              boolean autoDelete,
                                              VirtualHost virtualHost, final FieldTable arguments)
            throws AMQException
    {
        final int priorities = arguments == null ? 1 : arguments.containsKey(X_QPID_PRIORITIES) ? arguments.getInteger(X_QPID_PRIORITIES) : 1;
        String conflationKey = null;

        if(arguments != null && (arguments.containsKey(QPID_LAST_VALUE_QUEUE) || arguments.containsKey(QPID_LAST_VALUE_QUEUE_KEY)))
        {
            conflationKey = arguments.getString(QPID_LAST_VALUE_QUEUE_KEY);
            if(conflationKey == null)
            {
                conflationKey = QPID_LVQ_KEY;
            }
        }

        AMQQueue q = null;
        if(conflationKey != null)
        {
            q = new ConflationQueue(name, durable, owner, autoDelete, virtualHost, new AMQShortString(conflationKey));
        }
        else if(priorities > 1)
        {
            q = new AMQPriorityQueue(name, durable, owner, autoDelete, virtualHost, priorities);
        }
        else
        {
            q = new SimpleAMQQueue(name, durable, owner, autoDelete, virtualHost);
        }

        //Register the new queue
        virtualHost.getQueueRegistry().registerQueue(q);
        QueueConfiguration qConfig = virtualHost.getConfiguration().getQueueConfiguration(name.asString());
        q.configure(qConfig);

        if(arguments != null)
        {
            for(QueueProperty p : DECLAREABLE_PROPERTIES)
            {
                if(arguments.containsKey(p.getArgumentName()))
                {
                    p.setPropertyValue(q, arguments.get(p.getArgumentName()));
                }
            }
        }

        boolean dlqArgPresent = (arguments != null && (arguments.containsKey(X_QPID_DLQ_ENABLED)));

        if(dlqArgPresent || qConfig.isDeadLetterQueueEnabled())
        {
            //verify that the argument isn't explicitly disabling DLQ for this queue.
            boolean dlqEnabled = true;
            if(dlqArgPresent)
            {
                dlqEnabled = arguments.getBoolean(X_QPID_DLQ_ENABLED);
            }

            //feature is not to be enabled for temporary queues or when explicitly disabled by argument
            if(!q.isAutoDelete() && dlqEnabled) 
            {
                ServerConfiguration serverConfig = ApplicationRegistry.getInstance().getConfiguration();
                AMQShortString dlExchangeName = new AMQShortString(name + serverConfig.getDeadLetterExchangeSuffix());
                AMQShortString dlQueueName = new AMQShortString(name + serverConfig.getDeadLetterQueueSuffix());

                ExchangeRegistry exchangeRegistry = virtualHost.getExchangeRegistry();
                ExchangeFactory exchangeFactory = virtualHost.getExchangeFactory();
                QueueRegistry queueRegistry = virtualHost.getQueueRegistry();

                Exchange dlExchange = null;
                synchronized(exchangeRegistry)
                {
                    dlExchange = exchangeRegistry.getExchange(dlExchangeName);

                    if(dlExchange == null)
                    {
                        dlExchange = exchangeFactory.createExchange(dlExchangeName, 
                                ExchangeDefaults.FANOUT_EXCHANGE_CLASS, true, false, 0);

                        exchangeRegistry.registerExchange(dlExchange);

                        //enter the dle in the persistent store
                        virtualHost.getMessageStore().createExchange(dlExchange);
                    }
                }

                AMQQueue dlQueue = null;
                synchronized(queueRegistry)
                {
                    dlQueue = queueRegistry.getQueue(dlQueueName);

                    if(dlQueue == null)
                    {
                        //set args to disable DLQ'ing from the DLQ itself, preventing loops etc
                        FieldTable args = new FieldTable();
                        args.setBoolean(X_QPID_DLQ_ENABLED, false);
                        
                        dlQueue = createAMQQueueImpl(dlQueueName, true, owner, false, virtualHost, args);

                        //enter the dlq in the persistent store
                        virtualHost.getMessageStore().createQueue(dlQueue, args);
                    }
                }

                //ensure the queue is bound to the exchange
                if(!dlExchange.isBound(DLQ_ROUTING_KEY, dlQueue))
                {
                    dlQueue.bind(dlExchange, DLQ_ROUTING_KEY, null);
                }
                
                q.setAlternateExchange(dlExchange);
            }

        }
        
        return q;
    }

    public static AMQQueue createAMQQueueImpl(QueueConfiguration config, VirtualHost host) throws AMQException
    {
        AMQShortString queueName = new AMQShortString(config.getName());

        boolean durable = config.getDurable();
        boolean autodelete = config.getAutoDelete();
        AMQShortString owner = (config.getOwner() != null) ? new AMQShortString(config.getOwner()) : null;
        FieldTable arguments = null;
        boolean priority = config.getPriority();
        int priorities = config.getPriorities();
        if(priority || priorities > 0)
        {
            if(arguments == null)
            {
                arguments = new FieldTable();
            }
            if (priorities < 0)
            {
                priorities = 10;
            }
            arguments.put(new AMQShortString("x-qpid-priorities"), priorities);
        }
        if(config.isLVQ() || config.getLVQKey() != null)
        {
            if(arguments == null)
            {
                arguments = new FieldTable();
            }
            arguments.setInteger(QPID_LAST_VALUE_QUEUE, 1);
            arguments.setString(QPID_LAST_VALUE_QUEUE_KEY, config.getLVQKey() == null ? QPID_LVQ_KEY : config.getLVQKey());

        }
        if (!config.getAutoDelete() && config.isDeadLetterQueueEnabled())
        {
            if(arguments == null)
            {
                arguments = new FieldTable();
            }
            
            arguments.setBoolean(X_QPID_DLQ_ENABLED, true);
        }

        AMQQueue q = createAMQQueueImpl(queueName, durable, owner, autodelete, host, arguments);
        q.configure(config);
        return q;
    }
}
