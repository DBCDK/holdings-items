/*
 * dbc-holdings-items-update-webservice
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043*
 *
 * This file is part of dbc-holdings-items-update-webservice.
 *
 * dbc-holdings-items-update-webservice is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * dbc-holdings-items-update-webservice is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with dbc-holdings-items-update-webservice.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

import com.sun.xml.ws.developer.ValidationErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Soap XML validation handler
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class WsdlValidationHandler extends ValidationErrorHandler {

    public static final String WARNING = "WsdlErrorHandler.WARNING";
    public static final String ERROR = "WsdlErrorHandler.ERROR";
    public static final String FATAL = "WsdlErrorHandler.FATAL";

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        packet.invocationProperties.put(WARNING, exception);
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        packet.invocationProperties.put(ERROR, exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        packet.invocationProperties.put(FATAL, exception);
    }

}
