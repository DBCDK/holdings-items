/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-solr-indexer
 *
 * holdings-items-solr-indexer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-solr-indexer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.kafkabridge;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@ApplicationScoped
@SuppressWarnings("PMD.UnusedPrivateField")
public class EntityManagerInject {

    @Produces
    @PersistenceContext(unitName = "holdingsItems_PU")
    private EntityManager em;
}
