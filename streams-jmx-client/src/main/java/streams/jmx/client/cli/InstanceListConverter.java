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

import java.util.HashSet;
import java.util.Set;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * IParameterValidator that verifies that a parameter value is a filesystem path
 * that points to an existing file.
 */
public class InstanceListConverter implements IStringConverter<Set<String>> {

    @Override
    public Set<String> convert(String instanceList) {
    		return convertInstanceList(instanceList);
    }
    
    public static Set<String> convertInstanceList(String instanceList) throws ParameterException {
    	
	    	String [] instances = instanceList.split(",");
	    	Set<String> instanceSet = new HashSet<>();
	    	for (String instance : instances) {
	    		if (instance.length() > 0) {
	    			instanceSet.add(instance);
	    		}
	    	}
	    	return instanceSet;
    }
}
