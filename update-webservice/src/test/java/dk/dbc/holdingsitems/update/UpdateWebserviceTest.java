/*
 * Copyright (C) 2017-2018 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items
 *
 * holdings-items is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.update;

import dk.dbc.forsrights.client.ForsRightsException;
import dk.dbc.oss.ns.holdingsitemsupdate.Authentication;
import org.junit.Test;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class UpdateWebserviceTest {

    @Test(timeout = 2_000L)
    public void testAuthEnabledAuthOk() throws Exception {
        System.out.println("testAuthEnabledAuthOk");
        makeAuthTestRequest("user/123456/pass", 123456, false);
    }

    @Test(timeout = 2_000L)
    public void testAuthDisabledAuthFailure() throws Exception {
        System.out.println("testAuthDisabledAuthFailure");
        makeAuthTestRequest("user/123456/ERROR", 123456, true);
    }

    @Test(timeout = 2_000L, expected = AuthenticationException.class)
    public void testAuthEnabledAuthFailure() throws Exception {
        System.out.println("testAuthEnabledAuthFailure");
        makeAuthTestRequest("user/123456/ERROR", 123456, false);
    }

    @Test(timeout = 2_000L)
    public void testAuthDisabledNoAuth() throws Exception {
        System.out.println("testAuthDisabledNoAuth");
        makeAuthTestRequest(null, 123456, true);
    }

    @Test(timeout = 2_000L, expected = AuthenticationException.class)
    public void testAuthEnabledNoAuth() throws Exception {
        System.out.println("testAuthEnabledNoAuth");
        makeAuthTestRequest(null, 123456, false);
    }

    @Test(timeout = 2_000L)
    public void testAuthDisabledAuthMisMatch() throws Exception {
        System.out.println("testAuthDisabledAuthMisMatch");
        makeAuthTestRequest("user/654321/pass", 123456, true);
    }

    @Test(timeout = 2_000L, expected = AuthenticationException.class)
    public void testAuthEnabledAuthMisMatch() throws Exception {
        System.out.println("testAuthEnabledAuthMisMatch");
        makeAuthTestRequest("user/654321/pass", 123456, false);
    }

    public static void makeAuthTestRequest(String auth, int agencyId, boolean disableAuth) {
        UpdateWebservice updateWebservice = new UpdateWebservice();
        updateWebservice.config = new Config() {
            @Override
            public String getRightsGroup() {
                return "";
            }

            @Override
            public String getRightsName() {
                return "";
            }

            @Override
            public boolean getDisableAuthentication() {
                return disableAuth;
            }

        };
        updateWebservice.validator = new AccessValidator() {
            @Override
            public String validate(Authentication auth, String rightsGroup, String rightsName) throws ForsRightsException {
                if (auth == null) {
                    return null; // No auth supplied
                }
                if (!auth.getPasswordAut().equals("pass")) {
                    return null; // Password failure
                }
                return auth.getGroupIdAut(); // Login success
            }
        };

        UpdateRequest updateRequest = new UpdateRequest(null) { // No timers used
            @Override
            public Authentication getAuthentication() {
                if (auth == null) {
                    return null;
                }
                Authentication authentication = new Authentication();
                String[] parts = auth.split("/", 3);
                authentication.setUserIdAut(parts[0]);
                authentication.setGroupIdAut(parts[1]);
                authentication.setPasswordAut(parts[2]);
                return authentication;
            }

            @Override
            public int getAgencyId() {
                return agencyId;
            }

            @Override
            public String getTrakingId() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getQueueListOld() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String getQueueList() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void processBibliograhicItems() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };

        updateWebservice.userValidation(updateRequest);
    }

}
