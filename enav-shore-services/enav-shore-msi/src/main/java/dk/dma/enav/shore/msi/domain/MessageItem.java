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
package dk.dma.enav.shore.msi.domain;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import dk.dma.enav.shore.common.domain.BaseEntity;

@Entity
public class MessageItem extends BaseEntity<Integer> {
    
    private static final long serialVersionUID = 1L;
    
    private String amplifyingRemarks;
    private String keySubject;
    @ManyToOne
    private NavwarnMessage navwarnMessage;

    public MessageItem() {
        
    }
    
    public String getAmplifyingRemarks() {
        return amplifyingRemarks;
    }
    
    public void setAmplifyingRemarks(String amplifyingRemarks) {
        this.amplifyingRemarks = amplifyingRemarks;
    }
    
    public String getKeySubject() {
        return keySubject;
    }
    
    public void setKeySubject(String keySubject) {
        this.keySubject = keySubject;
    }
    
    public NavwarnMessage getNavwarnMessage() {
        return navwarnMessage;
    }
    
    public void setNavwarnMessage(NavwarnMessage navwarnMessage) {
        this.navwarnMessage = navwarnMessage;
    }
    
}
