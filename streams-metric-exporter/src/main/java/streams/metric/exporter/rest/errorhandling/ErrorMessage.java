// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package streams.metric.exporter.rest.errorhandling;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import streams.metric.exporter.error.StreamsTrackerException;

/*
 * ErrorMessage
 * Allows Jersey to convert error message into output type (e.g. JSON)
 */
public class ErrorMessage {
    int status;
    int code;
    String message;
    String developerMessage;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }

    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public ErrorMessage(StreamsTrackerException ex) {
        setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        setCode(ex.getErrorCode().getCode());
        setMessage(ex.getErrorCode().getDescription());
        setDeveloperMessage(ex.getMessage());
    }

    public ErrorMessage(NotFoundException ex) {
        setStatus(Response.Status.NOT_FOUND.getStatusCode());
        setMessage(ex.getMessage());
    }

    public ErrorMessage() {
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newline = System.getProperty("line.separator");

        result.append("status: " + this.getStatus());
        result.append(newline);
        result.append("code: " + this.getCode());
        result.append(newline);
        result.append("mesage: " + this.getMessage());
        result.append(newline);
        result.append("developerMessage: " + this.getDeveloperMessage());
        return result.toString();
    }

}
