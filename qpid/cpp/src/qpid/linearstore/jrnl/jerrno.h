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

#ifndef QPID_LEGACYSTORE_JRNL_JERRNO_H
#define QPID_LEGACYSTORE_JRNL_JERRNO_H

namespace qpid
{
namespace qls_jrnl
{
class jerrno;
}}

#include <map>
#include <stdint.h>
#include <string>

namespace qpid
{
namespace qls_jrnl
{

    /**
    * \class jerrno
    * \brief Class containing static error definitions and static map for error messages.
    */
    class jerrno
    {
        static std::map<uint32_t, const char*> _err_map; ///< Map of error messages
        static std::map<uint32_t, const char*>::iterator _err_map_itr; ///< Iterator
        static bool _initialized;                       ///< Dummy flag, used to initialise map.

    public:
        // generic errors
        static const uint32_t JERR__MALLOC;            ///< Buffer memory allocation failed
        static const uint32_t JERR__UNDERFLOW;         ///< Underflow error
        static const uint32_t JERR__NINIT;             ///< Operation on uninitialized class
        static const uint32_t JERR__AIO;               ///< AIO failure
        static const uint32_t JERR__FILEIO;            ///< File read or write failure
        static const uint32_t JERR__RTCLOCK;           ///< Reading real-time clock failed
        static const uint32_t JERR__PTHREAD;           ///< pthread failure
        static const uint32_t JERR__TIMEOUT;           ///< Timeout waiting for an event
        static const uint32_t JERR__UNEXPRESPONSE;     ///< Unexpected response to call or event
        static const uint32_t JERR__RECNFOUND;         ///< Record not found
        static const uint32_t JERR__NOTIMPL;           ///< Not implemented

        // class jcntl
        static const uint32_t JERR_JCNTL_STOPPED;      ///< Operation on stopped journal
        static const uint32_t JERR_JCNTL_READONLY;     ///< Write operation on read-only journal
        static const uint32_t JERR_JCNTL_AIOCMPLWAIT;  ///< Timeout waiting for AIOs to complete
        static const uint32_t JERR_JCNTL_UNKNOWNMAGIC; ///< Found record with unknown magic
        static const uint32_t JERR_JCNTL_NOTRECOVERED; ///< Req' recover() to be called first
        static const uint32_t JERR_JCNTL_RECOVERJFULL; ///< Journal data files full, cannot write
        static const uint32_t JERR_JCNTL_OWIMISMATCH;  ///< OWI change found in unexpected location

        // class jdir
        static const uint32_t JERR_JDIR_NOTDIR;        ///< Exists but is not a directory
        static const uint32_t JERR_JDIR_MKDIR;         ///< Directory creation failed
        static const uint32_t JERR_JDIR_OPENDIR;       ///< Directory open failed
        static const uint32_t JERR_JDIR_READDIR;       ///< Directory read failed
        static const uint32_t JERR_JDIR_CLOSEDIR;      ///< Directory close failed
        static const uint32_t JERR_JDIR_RMDIR;         ///< Directory delete failed
        static const uint32_t JERR_JDIR_NOSUCHFILE;    ///< File does not exist
        static const uint32_t JERR_JDIR_FMOVE;         ///< File move failed
        static const uint32_t JERR_JDIR_STAT;          ///< File stat failed
        static const uint32_t JERR_JDIR_UNLINK;        ///< File delete failed
        static const uint32_t JERR_JDIR_BADFTYPE;      ///< Bad or unknown file type (stat mode)

        // class fcntl
//        static const uint32_t JERR_FCNTL_OPENWR;       ///< Unable to open file for write
//        static const uint32_t JERR_FCNTL_WRITE;        ///< Unable to write to file
//        static const uint32_t JERR_FCNTL_CLOSE;        ///< File close failed
//        static const uint32_t JERR_FCNTL_FILEOFFSOVFL; ///< Increased offset past file size
//        static const uint32_t JERR_FCNTL_CMPLOFFSOVFL; ///< Increased cmpl offs past subm offs
//        static const uint32_t JERR_FCNTL_RDOFFSOVFL;   ///< Increased read offs past write offs

        // class lfmgr
//        static const uint32_t JERR_LFMGR_BADAEFNUMLIM; ///< Bad auto-expand file number limit
//        static const uint32_t JERR_LFMGR_AEFNUMLIMIT;  ///< Exceeded auto-expand file number limit
//        static const uint32_t JERR_LFMGR_AEDISABLED;   ///< Attempted to expand with auto-expand disabled

        // class rrfc
//        static const uint32_t JERR_RRFC_OPENRD;        ///< Unable to open file for read

        // class jrec, enq_rec, deq_rec, txn_rec
        static const uint32_t JERR_JREC_BADRECHDR;     ///< Invalid data record header
        static const uint32_t JERR_JREC_BADRECTAIL;    ///< Invalid data record tail

        // class wmgr
        static const uint32_t JERR_WMGR_BADPGSTATE;    ///< Page buffer in illegal state.
        static const uint32_t JERR_WMGR_BADDTOKSTATE;  ///< Data token in illegal state.
        static const uint32_t JERR_WMGR_ENQDISCONT;    ///< Enq. new dtok when previous part compl.
        static const uint32_t JERR_WMGR_DEQDISCONT;    ///< Deq. new dtok when previous part compl.
        static const uint32_t JERR_WMGR_DEQRIDNOTENQ;  ///< Deq. rid not enqueued

        // class rmgr
        static const uint32_t JERR_RMGR_UNKNOWNMAGIC;  ///< Found record with unknown magic
        static const uint32_t JERR_RMGR_RIDMISMATCH;   ///< RID mismatch between rec and dtok
        //static const uint32_t JERR_RMGR_FIDMISMATCH;   ///< FID mismatch between emap and rrfc
        static const uint32_t JERR_RMGR_ENQSTATE;      ///< Attempted read when wstate not ENQ
        static const uint32_t JERR_RMGR_BADRECTYPE;    ///< Attempted op on incorrect rec type

        // class data_tok
        static const uint32_t JERR_DTOK_ILLEGALSTATE;  ///< Attempted to change to illegal state
//         static const uint32_t JERR_DTOK_RIDNOTSET;     ///< Record ID not set

        // class enq_map, txn_map
        static const uint32_t JERR_MAP_DUPLICATE;      ///< Attempted to insert using duplicate key
        static const uint32_t JERR_MAP_NOTFOUND;       ///< Key not found in map
        static const uint32_t JERR_MAP_LOCKED;         ///< rid locked by pending txn

        // class jinf
//        static const uint32_t JERR_JINF_CVALIDFAIL;    ///< Compatibility validation failure
//        static const uint32_t JERR_JINF_NOVALUESTR;    ///< No value attr found in jinf file
//        static const uint32_t JERR_JINF_BADVALUESTR;   ///< Bad format for value attr in jinf file
//        static const uint32_t JERR_JINF_JDATEMPTY;     ///< Journal data files empty
//        static const uint32_t JERR_JINF_TOOMANYFILES;  ///< Too many journal data files
//        static const uint32_t JERR_JINF_INVALIDFHDR;   ///< Invalid file header
//        static const uint32_t JERR_JINF_STAT;          ///< Error while trying to stat a file
//        static const uint32_t JERR_JINF_NOTREGFILE;    ///< Target file is not a regular file
//        static const uint32_t JERR_JINF_BADFILESIZE;   ///< File is of incorrect or unexpected size
//        static const uint32_t JERR_JINF_OWIBAD;        ///< OWI inconsistent (>1 transition in non-ae journal)
//        static const uint32_t JERR_JINF_ZEROLENFILE;   ///< Journal info file is zero length (empty).

        // Negative returns for some functions
        static const int32_t AIO_TIMEOUT;               ///< Timeout waiting for AIO return
        static const int32_t LOCK_TAKEN;                ///< Attempted to take lock, but it was taken by another thread
        /**
        * \brief Method to access error message from known error number.
        */
        static const char* err_msg(const uint32_t err_no) throw ();

    private:
        /**
        * \brief Static function to initialize map.
        */
        static bool __init();
    };

}}

#endif // ifndef QPID_LEGACYSTORE_JRNL_JERRNO_H
