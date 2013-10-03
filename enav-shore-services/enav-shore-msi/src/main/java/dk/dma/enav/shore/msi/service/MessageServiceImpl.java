/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.enav.shore.msi.service;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;

import dk.dma.enav.shore.msi.dao.MessageDao;
import dk.dma.enav.shore.msi.domain.Message;
import dk.dma.enav.shore.msi.domain.NavwarnMessage;

@Stateless
public class MessageServiceImpl implements MessageService {
    
    @Inject
    private Logger log;
    
    @EJB
    private MessageDao messageDao;
    
    @Override
    public void create(NavwarnMessage navwarnMessage) {
        log.info("Creating navwarn message");
        messageDao.saveEntity(navwarnMessage);
    }

    @Override
    public List<Message> getAll() {
        return messageDao.getAll(Message.class);
    }

}