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
package org.apache.qpid.server.handler;

import org.apache.log4j.Logger;
import org.apache.qpid.AMQException;
import org.apache.qpid.AMQChannelException;
import org.apache.qpid.AMQConnectionException;
import org.apache.qpid.AMQUnknownExchangeType;
import org.apache.qpid.protocol.AMQConstant;
import org.apache.qpid.framing.AMQFrame;
import org.apache.qpid.framing.ExchangeDeclareBody;
import org.apache.qpid.framing.ExchangeDeclareOkBody;
import org.apache.qpid.server.exchange.Exchange;
import org.apache.qpid.server.exchange.ExchangeRegistry;
import org.apache.qpid.server.exchange.ExchangeFactory;
import org.apache.qpid.server.protocol.AMQMethodEvent;
import org.apache.qpid.server.protocol.AMQProtocolSession;
import org.apache.qpid.server.queue.QueueRegistry;
import org.apache.qpid.server.state.AMQStateManager;
import org.apache.qpid.server.state.StateAwareMethodListener;
import org.apache.qpid.server.registry.ApplicationRegistry;

public class ExchangeDeclareHandler implements StateAwareMethodListener<ExchangeDeclareBody>
{
    private static final Logger _logger = Logger.getLogger(ExchangeDeclareHandler.class);

    private static final ExchangeDeclareHandler _instance = new ExchangeDeclareHandler();

    public static ExchangeDeclareHandler getInstance()
    {
        return _instance;
    }

    private final ExchangeFactory exchangeFactory;

    private ExchangeDeclareHandler()
    {
        exchangeFactory = ApplicationRegistry.getInstance().getExchangeFactory();
    }

    public void methodReceived(AMQStateManager stateManager, QueueRegistry queueRegistry,
                               ExchangeRegistry exchangeRegistry, AMQProtocolSession protocolSession,
                               AMQMethodEvent<ExchangeDeclareBody> evt) throws AMQException
    {
        final ExchangeDeclareBody body = evt.getMethod();
        if (_logger.isDebugEnabled())
        {
            _logger.debug("Request to declare exchange of type " + body.type + " with name " + body.exchange);
        }
        synchronized(exchangeRegistry)
        {
            Exchange exchange = exchangeRegistry.getExchange(body.exchange);



            if (exchange == null)
            {
                if(body.passive && ((body.type == null) || body.type.length() ==0))
                {
                    throw new AMQChannelException(AMQConstant.NOT_FOUND.getCode(), "Unknown exchange: " + body.exchange,body.getClazz(), body.getMethod(),body.getMajor(),body.getMinor());                    
                }
                else
                {
                    try
                    {

                    exchange = exchangeFactory.createExchange(body.exchange, body.type, body.durable,
                                                              body.passive, body.ticket);
                    exchangeRegistry.registerExchange(exchange);
                    }
                    catch(AMQUnknownExchangeType e)
                    {
                        throw new AMQConnectionException(AMQConstant.COMMAND_INVALID.getCode(), "Unknown exchange: " + body.exchange,body.getClazz(), body.getMethod(),body.getMajor(),body.getMinor(),e);
                    }
                }
            }
            else if (!exchange.getType().equals(body.type))
            {

                throw new AMQConnectionException(AMQConstant.NOT_ALLOWED.getCode(), "Attempt to redeclare exchange: " + body.exchange + " of type " + exchange.getType() + " to " + body.type +".",body.getClazz(), body.getMethod(),body.getMajor(),body.getMinor());    
            }

        }
        if(!body.nowait)
        {
            // AMQP version change: Hardwire the version to 0-8 (major=8, minor=0)
            // TODO: Connect this to the session version obtained from ProtocolInitiation for this session.
            // Be aware of possible changes to parameter order as versions change.
            AMQFrame response = ExchangeDeclareOkBody.createAMQFrame(evt.getChannelId(), (byte)8, (byte)0);
            protocolSession.writeFrame(response);
        }
    }
}
