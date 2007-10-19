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
 
#include "ManagementObject.h"

using namespace qpid::framing;
using namespace qpid::broker;

void ManagementObject::schemaItem (Buffer&     buf,
				   uint8_t     typeCode,
				   std::string name,
				   std::string description,
				   bool        isConfig)
{
    buf.putOctet       (isConfig ? 1 : 0);
    buf.putOctet       (typeCode);
    buf.putShortString (name);
    buf.putShortString (description);
}

void ManagementObject::schemaListEnd (Buffer& buf)
{
    buf.putOctet (0xFF);
}
