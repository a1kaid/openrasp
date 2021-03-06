/*
 * Copyright 2017-2018 Baidu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.openrasp.cloud.model;

public enum ErrorType {
    REGISTER_ERROR(2008,"Cloud Control Registered Failed"),
    HEARTBEAT_ERROR(2009,"Cloud Control Send HeartBeat Failed"),
    STATISTICSREPORT_ERROR(3000,"Cloud Control Statistics,Report Failed"),
    HTTPAPPENDER_ERROR(3001,"Cloud Control Log Upload Failed");
    public int code;
    public String name;
    ErrorType(int code,String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode(){
        return code;
    }

    public String getName(){
        return name;
    }

    @Override
    public String toString() {
        return code+":"+name;
    }
}
