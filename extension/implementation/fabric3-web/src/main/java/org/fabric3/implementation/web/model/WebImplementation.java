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
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.implementation.web.model;

import java.net.URI;

import org.fabric3.api.model.type.component.Implementation;

/**
 * Model object for a web component.
 */
public class WebImplementation extends Implementation<WebComponentType> {
    private URI uri;

    /**
     * Default constructor. Used to create a web component implementation whose web app context URL will be constructed using the component URI.
     */
    public WebImplementation() {
    }

    /**
     * Constructor. Used to create a web component implementation whose web app context URL will be constructed using the component URI.
     *
     * @param uri the URI used when creating the web app context URL.
     */
    public WebImplementation(URI uri) {
        this.uri = uri;
    }

    public String getType() {
        return "web";
    }

    public URI getUri() {
        return uri;
    }

}
