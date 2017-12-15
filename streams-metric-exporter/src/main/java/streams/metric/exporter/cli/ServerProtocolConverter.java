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

package streams.metric.exporter.cli;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import streams.metric.exporter.rest.Protocol;

/**
 * IParameterValidator that verifies that a parameter value is a filesystem path
 * that points to an existing file.
 */
public class ServerProtocolConverter implements IStringConverter<Protocol> {

    @Override
    public Protocol convert(String value) throws ParameterException {
    	return convertProtocol(value);
    }
    
    public static Protocol convertProtocol(String value) throws ParameterException {
    	
    	if (value.equalsIgnoreCase(Protocol.HTTP.toString())) {
    		return Protocol.HTTP;
    	} else if (value.equalsIgnoreCase(Protocol.HTTPS.toString())) {
    		return Protocol.HTTPS;
    	} else {
            throw new ParameterException(String.format("%s is not a valid protocol.  Valid values include [http|https]", value));
        }
    }
    
}
