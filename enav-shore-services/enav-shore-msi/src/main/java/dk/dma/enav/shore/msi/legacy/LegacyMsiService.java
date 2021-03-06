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
package dk.dma.enav.shore.msi.legacy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.slf4j.Logger;

import dk.dma.enav.shore.msi.domain.GeneralCategory;
import dk.dma.enav.shore.msi.domain.Message;
import dk.dma.enav.shore.msi.domain.MessageCategory;
import dk.dma.enav.shore.msi.domain.MessageItem;
import dk.dma.enav.shore.msi.domain.MessageLocation;
import dk.dma.enav.shore.msi.domain.MessageSeriesIdentifier;
import dk.dma.enav.shore.msi.domain.MessageType;
import dk.dma.enav.shore.msi.domain.NavwarnMessage;
import dk.dma.enav.shore.msi.domain.Point;
import dk.dma.enav.shore.msi.domain.SpecificCategory;
import dk.dma.enav.shore.msi.domain.MessageLocation.LocationType;
import dk.dma.enav.shore.msi.service.MessageService;
import dk.frv.enav.msi.ws.warning.MsiService;
import dk.frv.enav.msi.ws.warning.WarningService;
import dk.frv.msiedit.core.webservice.message.MsiDto;
import dk.frv.msiedit.core.webservice.message.PointDto;

/**
 * Provides an interface for fetching active MSI warnings from the legacy MSI system.
 * <p>
 * Sets up a timer service which performs the legacy import every 5 minutes.
 */
@Singleton
@Startup
public class LegacyMsiService {
    
    private static final String MSI_WSDL = "/wsdl/warning.wsdl";
    
    @Inject
    Logger log;
    
    @EJB
    MessageService messageService;
    
    //String endpoint = "http://msi-beta.e-navigation.net/msi/ws/warning";
    String endpoint = "http://msi.dma.dk/msi/ws/warning";
    String countries = "DK";

    private MsiService msiService;
    
    /**
     * Called when the bean is initialized
     */
    @PostConstruct
    public void init() {
        msiService = new WarningService(
                getClass().getResource(MSI_WSDL), 
                new QName("http://enav.frv.dk/msi/ws/warning", "WarningService"))
                .getMsiServiceBeanPort();
        log.info("Initialized MSI webservice");
    }

    /**
     * Returns the current list of active MSI warnings
     * @return the current list of active MSI warnings
     */
    public List<MsiDto> getActiveWarnings() {
        
        // Update the WS endpoint
        ((BindingProvider) msiService).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        
        List<MsiDto> result = new ArrayList<>();

        boolean error = false;
        for (String country : countries.split(",")) {
            int count = 0;
            try {
                for (MsiDto md : msiService.getActiveWarningCountry(country).getItem()) {
                    result.add(md);
                    count++;
                }
            } catch (Throwable t) {
                log.error("Error reading warnings from MSI service: " + endpoint + " for country: " + country, t);
                error = true;
            }
            log.info("Read " + count + " warnings from MSI provider: " + country);
        }
        log.info("Read " + result.size() + " warnings from MSI service: " + endpoint);
        if(error) {
            log.error("There was a problem reading MSI warnings from endpoint " + endpoint);
            throw new RuntimeException();
        }
        return result;

    }
    
    /**
     * Called every 5 minutes to update the legacy MSI warnings
     */
    @Schedule(persistent=false, second="13", minute="*/5", hour="*", dayOfWeek="*", year="*")
    public void updateLegacyWarnings() {
        
        List<MsiDto> warnings = getActiveWarnings();
        log.info("Fetched " + warnings.size() + " legacy MSI warnings");
        
        for (MsiDto msi : warnings) {
            // Either create a new MSI or update an existing
            Message msg = messageService.findByMessageSeriesId(
                    msi.getMessageId(), 
                    msi.getCreated().toGregorianCalendar().get(Calendar.YEAR), 
                    msi.getOrganisation());
            
            if (msg == null) {
                // Create a new message
                createNewNavwarnMessage(msi);
            } else {
                // TODO
            }   
        }
    } 
    
    /**
     * Create and persist a new MSI warning from a legacy warning
     * @param msi the legacy warning
     */
    private void createNewNavwarnMessage(MsiDto msi) {
        NavwarnMessage message = new NavwarnMessage();
        
        // Message series identifier
        MessageSeriesIdentifier identifier = new MessageSeriesIdentifier();
        identifier.setAuthority(msi.getOrganisation());
        identifier.setYear(msi.getCreated().toGregorianCalendar().get(Calendar.YEAR));
        identifier.setNumber(msi.getMessageId());
        identifier.setType(MessageType.NAVAREA_WARNING);

        // Tie to message
        message.setSeriesIdentifier(identifier);
        identifier.setMessage(message);

        // Message
        message.setGeneralArea(msi.getAreaEnglish());
        message.setLocality(msi.getSubarea());
        message.setIssueDate(msi.getValidFrom().toGregorianCalendar().getTime());

        // NavwarnMessage
        if (msi.getDeleted() != null) {
            message.setCancellationDate(msi.getDeleted().toGregorianCalendar().getTime());
        }

        // MessageItem 's
        MessageItem item1 = new MessageItem();
        item1.setKeySubject(msi.getNavWarning());
        item1.setAmplifyingRemarks(msi.getEncText());

        MessageCategory cat1 = new MessageCategory();
        cat1.setGeneralCategory(GeneralCategory.ISOLATED_DANGERS);
        cat1.setSpecificCategory(SpecificCategory.OBSTRUCTION);
        cat1.setOtherCategory("");
        item1.setCategory(cat1);

        if (msi.getPoints() != null && msi.getPoints().getPoint().size() > 0) {
            MessageLocation loc1 = new MessageLocation(LocationType.POLYGON);
            for (PointDto p : msi.getPoints().getPoint()) {
                loc1.addPoint(new Point(p.getLatitude(), p.getLongitude()));                
            }
            item1.getLocation().add(loc1);
        }

        // Tie message items to navwarn message
        message.getMessageItem().add(item1);

        // Create navwarn message
        try {   
            message = messageService.create(message);
            log.info("Persisted new legacy MSI as navwarn message " + message.getId());
        } catch (Exception ex) {
            log.error("Error persisting legacy MSI " + msi, ex);
        }
    }
}
