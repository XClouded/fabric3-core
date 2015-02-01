/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fabric3.binding.ws.metro.generator.resolver;

import java.net.URL;

import org.fabric3.api.binding.ws.model.WsBinding;
import org.fabric3.api.host.Fabric3Exception;
import org.fabric3.spi.model.instance.LogicalBinding;

/**
 * Determines the endpoint of a URL based on the service binding metadata. This is used when determining a URL for a reference targeted using the SCA service
 * URL.
 */
public interface TargetUrlResolver {

    /**
     * Calculate the URL from the service binding metadata.
     *
     * @param binding the service binding
     * @return the URL
     * @throws Fabric3Exception if the URL cannot be created
     */
    URL resolveUrl(LogicalBinding<WsBinding> binding) throws Fabric3Exception;

}