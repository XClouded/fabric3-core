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
*/
package org.fabric3.binding.web.runtime.channel;

/**
 * Manages {@link ChannelSubscriber} and {@link ChannelPublisher} instances.
 *
 * @version $Rev: 9435 $ $Date: 2010-09-09 17:31:45 +0200 (Thu, 09 Sep 2010) $
 */
public interface PubSubManager {

    /**
     * Registers a publisher.
     *
     * @param path      the relative path of the channel the publisher sends events to.
     * @param publisher the publisher
     */
    void register(String path, ChannelPublisher publisher);

    /**
     * Registers a subscriber.
     *
     * @param path       the relative path of the channel the subscriber listens to.
     * @param subscriber the publisher
     */
    void register(String path, ChannelSubscriber subscriber);

    /**
     * Removes a publisher.
     *
     * @param path the relative path of the channel the subscriber listens to.
     * @return returns the publisher or null if not found
     */
    ChannelPublisher unregisterPublisher(String path);

    /**
     * Removes a subscriber.
     *
     * @param path the relative path of the channel the subscriber listens to.
     * @return returns the subscriber or null if not found
     */
    ChannelSubscriber unregisterSubscriber(String path);

    /**
     * Returns the publisher for the given channel.
     *
     * @param path the relative channel path
     * @return the publisher
     */
    ChannelPublisher getPublisher(String path);

    /**
     * Returns the subscriber for the given channel.
     *
     * @param path the relative channel path
     * @return the subscriber
     */
    ChannelSubscriber getSubscriber(String path);

}
