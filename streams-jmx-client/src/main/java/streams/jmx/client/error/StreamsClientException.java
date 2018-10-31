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

package streams.jmx.client.error;

public class StreamsClientException extends Exception {

    // required/suggested for all serializable classes
    private static final long serialVersionUID = 814863L;

    StreamsClientErrorCode errorCode = StreamsClientErrorCode.UNSPECIFIED_ERROR;

    public StreamsClientException() {
        super();
        this.errorCode = StreamsClientErrorCode.UNSPECIFIED_ERROR;
    }

    public StreamsClientException(StreamsClientErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public StreamsClientException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = StreamsClientErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsClientException(StreamsClientErrorCode errorCode,
            String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public StreamsClientException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = StreamsClientErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsClientException(StreamsClientErrorCode errorCode,
            String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;

    }

    public StreamsClientException(String message) {
        super(message);
        this.errorCode = StreamsClientErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsClientException(StreamsClientErrorCode errorCode,
            String message) {
        super(message);
        this.errorCode = errorCode;

    }

    public StreamsClientException(Throwable cause) {
        super(StreamsClientErrorCode.UNSPECIFIED_ERROR.getDescription(), cause);
        this.errorCode = StreamsClientErrorCode.UNSPECIFIED_ERROR;

    }

    public StreamsClientException(StreamsClientErrorCode errorCode,
            Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.errorCode = errorCode;

    }

    public StreamsClientErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(StreamsClientErrorCode errorCode) {
        this.errorCode = errorCode;
    }

}
