#ifndef __message_private_h__
#define __message_private_h__ 1
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

#include <qpid/dispatch/message.h>
#include <qpid/dispatch/alloc.h>
#include <qpid/dispatch/threading.h>

/**
 * Architecture of the message module:
 *
 *     +--------------+            +----------------------+
 *     |              |            |                      |
 *     | dx_message_t |----------->| dx_message_content_t |
 *     |              |     +----->|                      |
 *     +--------------+     |      +----------------------+
 *                          |                |
 *     +--------------+     |                |    +-------------+   +-------------+   +-------------+
 *     |              |     |                +--->| dx_buffer_t |-->| dx_buffer_t |-->| dx_buffer_t |--/
 *     | dx_message_t |-----+                     +-------------+   +-------------+   +-------------+
 *     |              |
 *     +--------------+
 *
 * The message module provides chained-fixed-sized-buffer storage of message content with multiple
 * references.  If a message is received and is to be queued for multiple destinations, there is only
 * one copy of the message content in memory but multiple lightweight references to the content.
 *
 */

typedef struct {
    dx_buffer_t *buffer;     // Buffer that contains the first octet of the field, null if the field is not present
    size_t       offset;     // Offset in the buffer to the first octet
    size_t       length;     // Length of the field or zero if unneeded
    size_t       hdr_length; // Length of the field's header (not included in the length of the field)
    int          parsed;     // non-zero iff the buffer chain has been parsed to find this field
} dx_field_location_t;


// TODO - consider using pointers to dx_field_location_t below to save memory
// TODO - we need a second buffer list for modified annotations and header
//        There are three message scenarios:
//            1) Received message is held and forwarded unmodified - single buffer list
//            2) Received message is held and modified before forwarding - two buffer lists
//            3) Message is composed internally - single buffer list
// TODO - provide a way to allocate a message without a lock for the link-routing case.
//        It's likely that link-routing will cause no contention for the message content.
//

typedef struct {
    sys_mutex_t         *lock;
    uint32_t             ref_count;                       // The number of messages referencing this
    dx_buffer_list_t     buffers;                         // The buffer chain containing the message
    dx_buffer_list_t     new_delivery_annotations;        // The buffer chain containing the new delivery annotations (MOVE TO MSG_PVT)
    dx_field_location_t  section_message_header;          // The message header list
    dx_field_location_t  section_delivery_annotation;     // The delivery annotation map
    dx_field_location_t  section_message_annotation;      // The message annotation map
    dx_field_location_t  section_message_properties;      // The message properties list
    dx_field_location_t  section_application_properties;  // The application properties list
    dx_field_location_t  section_body;                    // The message body: Data
    dx_field_location_t  section_footer;                  // The footer
    dx_field_location_t  field_user_id;                   // The string value of the user-id
    dx_field_location_t  field_to;                        // The string value of the to field
    dx_field_location_t  field_reply_to;                  // The string value of the reply_to field
    dx_field_location_t  body;                            // The body of the message
    dx_buffer_t         *parse_buffer;
    unsigned char       *parse_cursor;
    dx_message_depth_t   parse_depth;
    dx_parsed_field_t   *parsed_delivery_annotations;
} dx_message_content_t;

typedef struct {
    DEQ_LINKS(dx_message_t);   // Deque linkage that overlays the dx_message_t
    dx_message_content_t *content;
} dx_message_pvt_t;

ALLOC_DECLARE(dx_message_t);
ALLOC_DECLARE(dx_message_content_t);

#define MSG_CONTENT(m) (((dx_message_pvt_t*) m)->content)

#endif
