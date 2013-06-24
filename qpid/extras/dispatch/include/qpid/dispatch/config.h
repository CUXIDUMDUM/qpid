#ifndef __dispatch_config_h__
#define __dispatch_config_h__ 1
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

#include <stdint.h>

typedef struct dx_config_t dx_config_t;

int dx_config_item_count(const dx_config_t *config, const char *section);
const char *dx_config_item_value_string(const dx_config_t *config, const char *section, int index, const char* key);
uint32_t dx_config_item_value_int(const dx_config_t *config, const char *section, int index, const char* key);
int dx_config_item_value_bool(const dx_config_t *config, const char *section, int index, const char* key);

#endif
