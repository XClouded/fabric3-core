/*
 * Fabric3
 * Copyright (c) 2009-2012 Metaform Systems
 *
 * Fabric3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version, with the
 * following exception:
 *
 * Linking this software statically or dynamically with other
 * modules is making a combined work based on this software.
 * Thus, the terms and conditions of the GNU General Public
 * License cover the whole combination.
 *
 * As a special exception, the copyright holders of this software
 * give you permission to link this software with independent
 * modules to produce an executable, regardless of the license
 * terms of these independent modules, and to copy and distribute
 * the resulting executable under terms of your choice, provided
 * that you also meet, for each linked independent module, the
 * terms and conditions of the license of that module. An
 * independent module is a module which is not derived from or
 * based on this software. If you modify this software, you may
 * extend this exception to your version of the software, but
 * you are not obligated to do so. If you do not wish to do so,
 * delete this exception statement from your version.
 *
 * Fabric3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the
 * GNU General Public License along with Fabric3.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 * ----------------------------------------------------
 *
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 *
 */
package org.fabric3.spi.model.physical;

import java.io.Serializable;
import java.net.URI;

/**
 * Metadata for attaching a handler to an event stream.
 */
public class PhysicalHandlerDefinition implements Serializable {
    private static final long serialVersionUID = 660389044446376070L;
    private URI policyClassLoaderId;

    /**
     * Returns the classloader id for the contribution containing the handler. This may be the same as the wire classloader id if the policy is
     * contained in the same user contribution as the source component of the wire.
     *
     * @return the classloader id for the policy
     */
    public URI getPolicyClassLoaderId() {
        return policyClassLoaderId;
    }

    /**
     * Sets the classloader id for the contribution containing the handler. This may be the same as the wire classloader id if the policy is contained
     * in the same user contribution as the source component of the wire.
     *
     * @param id classloader id for the policy
     */
    public void setPolicyClassLoaderId(URI id) {
        this.policyClassLoaderId = id;
    }
}
