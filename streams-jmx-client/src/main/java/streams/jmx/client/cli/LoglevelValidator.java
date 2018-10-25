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

package streams.jmx.client.cli;

import org.apache.log4j.Level;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import streams.jmx.client.Constants;

public class LoglevelValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
    	if (!isValid(value)) {
            throw new ParameterException(String.format(Constants.INVALID_LOGLEVEL, value));
        }
    }
    
    /* Validate it can be converted to a valid log4j level */
    public static boolean isValid(String value) {
    	return ((value.equalsIgnoreCase(Level.FATAL.toString())) || 
    			(value.equalsIgnoreCase(Level.ERROR.toString())) ||
    			(value.equalsIgnoreCase(Level.WARN.toString())) ||
    			(value.equalsIgnoreCase(Level.INFO.toString())) ||
    			(value.equalsIgnoreCase(Level.DEBUG.toString())) ||
    			(value.equalsIgnoreCase(Level.TRACE.toString()))); 	
    }
    
}
