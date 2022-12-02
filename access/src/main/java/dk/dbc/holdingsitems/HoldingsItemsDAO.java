/*
 * Copyright (C) 2014-2018 DBC A/S (http://dbc.dk/)
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
package dk.dbc.holdingsitems;

import dk.dbc.holdingsitems.jpa.StatusCountEntity;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.holdingsitems.jpa.SupersedesEntity;
import dk.dbc.holdingsitems.jpa.VersionSort;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class HoldingsItemsDAO {

    private final EntityManager em;
    private final String trackingId;

    /**
     * Constructor
     *
     * @param em         EntityManager
     * @param trackingId tracking of updates
     */
    HoldingsItemsDAO(EntityManager em, String trackingId) {
        this.em = em;
        this.trackingId = trackingId;
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param em database connection
     * @return a HoldingsItemsDAO for the connection
     */
    public static HoldingsItemsDAO newInstance(EntityManager em) {
        return newInstance(em, "");
    }

    /**
     * Load a class and instantiate it based on the driver name from the
     * supplied connection
     *
     * @param em         database connection
     * @param trackingId tracking id for database updates
     * @return a HoldingsItemsDAO for the connection
     */
    public static HoldingsItemsDAO newInstance(EntityManager em, String trackingId) {
        return new HoldingsItemsDAO(em, trackingId);
    }

    /**
     * Get the tracking id
     *
     * @return The stored tracking id
     */
    public String getTrackingId() {
        return trackingId;
    }

    /**
     * Find all bibliographicrecordids that are not decommissioned, for a given
     * agency
     * <p>
     * This will be slow
     *
     * @param agencyId agency in question
     * @return collection of bibliographicrecordids for the given agency
     */
    public Set<String> getBibliographicIds(int agencyId) {
        return new HashSet<>(em.createQuery("SELECT h.bibliographicRecordId" +
                                            " FROM ItemEntity h" +
                                            " WHERE h.agencyId = :agencyId" +
                                            " GROUP BY h.agencyId, h.bibliographicRecordId",
                                            String.class)
                .setParameter("agencyId", agencyId)
                .getResultList());
    }

    /**
     * Get a list of "XXXXXX/YYYYY" strings for a bibliographic record id, where XXXXXX is an agencyId
     * and YYYYY is a branch.
     *
     * @param bibliographicRecordId a bibliographicrecordId
     * @return a list of agency/branch strings
     */
    public List<Object[]> getAgencyBranchStringsForBibliographicRecordId(String bibliographicRecordId) {
        return em.createQuery("SELECT h.agencyId, h.bibliographicRecordId, h.branch, h.status" +
                              " FROM ItemEntity h" +
                              " WHERE h.bibliographicRecordId = :bibliographicRecordId" +
                              " AND h.branch != ''")
                .setParameter("bibliographicRecordId", bibliographicRecordId)
                .getResultList();
    }

    /**
     * Find all bibliographicrecordids for a given agency
     * <p>
     * This will be slow
     *
     * @param agencyId agency in question
     * @return collection of bibliographicrecordids for the given agency
     */
    public Set<String> getBibliographicIdsIncludingDecommissioned(int agencyId) {
        return new HashSet<>(em.createQuery("SELECT h.bibliographicRecordId" +
                                            " FROM BibliographicItemEntity h" +
                                            " WHERE h.agencyId = :agencyId",
                                            String.class)
                .setParameter("agencyId", agencyId)
                .getResultList());
    }

    /**
     * Find all issueids for a given bibliographic record
     *
     * @param bibliographicRecordId the bibliographic record
     * @param agencyId              the agency in question
     * @return a collection of all the issueids for the record
     * @throws HoldingsItemsException When database communication fails
     */
    public Set<String> getIssueIds(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        BibliographicItemEntity entity = BibliographicItemEntity.fromUnLocked(em, agencyId, bibliographicRecordId);
        if (entity == null) {
            return Collections.emptySet();
        } else {
            return entity.stream().map(IssueEntity::getIssueId).collect(Collectors.toSet());
        }
    }

    /**
     * Find all item entities that match the given agencyId and itemId.
     * Normally there will be at most one, but there can be more if the same
     * item id is connected to the same
     * issueId / bibliographicRecordId. That is normally a data problem.
     *
     * @param agencyId id of library in question
     * @param itemId   item id to search for
     * @return a collection of ItemEntity objects that match the parameters.
     */
    public Set<ItemEntity> getItemsFromAgencyIdAndItemId(int agencyId, String itemId) {
        List<ItemEntity> itemList =
                em.createQuery("SELECT h" +
                               " FROM ItemEntity h" +
                               " WHERE h.agencyId = :agencyId" +
                               "  AND h.itemId = :itemId",
                               ItemEntity.class)
                        .setParameter("agencyId", agencyId)
                        .setParameter("itemId", itemId)
                        .getResultList();
        return new HashSet<>(itemList);
    }

    /**
     * Find all item entities that match the given branchId and itemId.
     *
     * @param agencyId              id of library in question
     * @param branchId              id of branch in question
     * @param bibliographicRecordId record id to search for
     * @return a collection of ItemEntity objects that match the parameters.
     */
    public Set<ItemEntity> getItemsFromBranchIdAndBibliographicRecordId(int agencyId, String branchId, String bibliographicRecordId) {
        return streamItemsFromAgencyAndBibliographicRecordId(agencyId, bibliographicRecordId)
                .filter(item -> branchId.equals(item.getBranchId()))
                .collect(Collectors.toSet());
    }

    /**
     * Get all items that match a given combination of agencyId and
     * bibliographicRecordId
     *
     * @param agencyId              id of a library (int)
     * @param bibliographicRecordId id of a bibliographic record (string)
     * @return the set of holdings items that match the arguments.
     */
    public Set<ItemEntity> getItemsFromAgencyAndBibliographicRecordId(int agencyId, String bibliographicRecordId) {
        return streamItemsFromAgencyAndBibliographicRecordId(agencyId, bibliographicRecordId)
                .collect(Collectors.toSet());
    }

    /**
     * Create / Get a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @param modified              timestamp to use for created/complete if a
     *                              new record is created
     * @return record collection object
     */
    public BibliographicItemEntity getRecordCollection(String bibliographicRecordId, int agencyId, Instant modified) {
        BibliographicItemEntity b = BibliographicItemEntity.from(
                em, agencyId, bibliographicRecordId, modified,
                modified == null ? null : LocalDateTime.ofInstant(modified, ZoneOffset.UTC).toLocalDate());
        if (b.isNew())
            b.setTrackingId(trackingId);
        return b;
    }

    /**
     * Create / Get a collection of items defined by id/library/orderId
     *
     * @param bibliographicRecordId part of the primary key
     * @param agencyId              part of the primary key
     * @return record collection object null if non-existing
     */
    public BibliographicItemEntity getRecordCollectionUnLocked(String bibliographicRecordId, int agencyId) {
        return BibliographicItemEntity.fromUnLocked(
                em, agencyId, bibliographicRecordId);
    }

    /**
     * Get a set of agencies that has holdings for a record
     *
     * @param bibliographicRecordId id of record
     * @return set of agencyId
     * @throws HoldingsItemsException When database communication fails
     */
    public Set<Integer> getAgenciesThatHasHoldingsFor(String bibliographicRecordId) throws HoldingsItemsException {
        return em.createQuery(
                "SELECT h.agencyId FROM ItemEntity h" +
                " WHERE h.bibliographicRecordId = :bibId" +
                " GROUP BY h.agencyId",
                Integer.class)
                .setParameter("bibId", bibliographicRecordId)
                .getResultStream()
                .collect(Collectors.toSet());
    }

    /**
     * Update bibliographic item note - should be called before items are
     * fetched
     *
     * @param note                  The text to apply to all issues
     * @param agencyId              owner
     * @param bibliographicRecordId owner
     * @param modified              modified timestamp
     * @throws HoldingsItemsException in case of a database error
     */
    public void updateBibliographicItemNote(String note, int agencyId, String bibliographicRecordId, Instant modified) throws HoldingsItemsException {
        BibliographicItemEntity item = BibliographicItemEntity.from(em, agencyId, bibliographicRecordId, modified, null);
        if (item.isNew() ||
            !item.getModified().isAfter(modified))
            item.setNote(note);
    }

    /**
     * Create a map of status to number of items with said status
     *
     * @param bibliographicRecordId key
     * @param agencyId              key
     * @return key-value pairs with status and number of that status
     * @throws HoldingsItemsException in case of a database error
     */
    public Map<Status, Long> getStatusFor(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        return streamItemsFromAgencyAndBibliographicRecordId(agencyId, bibliographicRecordId)
                .collect(Collectors.groupingBy(ItemEntity::getStatus, Collectors.counting()));
    }

    //TODO throws ClassCastException ...
    public StatusCountEntity getStatusCountsByAgency(int agency, String trackingId) throws HoldingsItemsException {
        return em.createQuery("SELECT i.status, COUNT(i) " +
                        "FROM ItemEntity i " +
                        "WHERE i.agencyId = :agencyId " +
                        "GROUP BY i.status",
                        StatusCountEntity.class)
                .setParameter("agencyId", agency)
                .getSingleResult();

        // Morten snakkede om at jeg skulle streame resultatet fordi der er tabel-lignende så jeg kan tage hver række
        // putte dem i et map ...
    }

    /**
     * Has a holding that is not decommissioned (ignore supersedes)
     *
     * @param bibliographicRecordId key
     * @param agencyId              key
     * @return if any holding is not 'decommissioned'
     * @throws HoldingsItemsException in case of a database error
     */
    public boolean hasLiveHoldings(String bibliographicRecordId, int agencyId) throws HoldingsItemsException {
        return streamItemsFromBibliographicEntity(BibliographicItemEntity.fromUnLocked(em, agencyId, bibliographicRecordId))
                .findAny()
                .isPresent();
    }

    private Stream<ItemEntity> streamItemsFromAgencyAndBibliographicRecordId(int agencyId, String bibliographicRecordId) {
        return streamItemsFromBibliographicEntity(BibliographicItemEntity.detachedWithSuperseded(em, agencyId, bibliographicRecordId));
    }

    private Stream<ItemEntity> streamItemsFromBibliographicEntity(BibliographicItemEntity entity) {
        if (entity == null) {
            return Stream.empty();
        } else {
            VersionSort versionSort = new VersionSort();
            return entity.stream()
                    .sorted(Comparator.comparing(IssueEntity::getIssueId, versionSort))
                    .flatMap(IssueEntity::stream)
                    .sorted(Comparator.comparing(ItemEntity::getItemId, versionSort));
        }
    }

    public String getActualBibliographicRecordId(String bibliographicRecordId) {
        SupersedesEntity superseded = em.find(SupersedesEntity.class, bibliographicRecordId);
        if (superseded == null) {
            return bibliographicRecordId;
        } else {
            return superseded.getSuperseding();
        }
    }

    /**
     * Service to enqueue from a supplier
     *
     * @return enqueue class
     * @throws HoldingsItemsException In case of a database error
     */
    public EnqueueService enqueueService() throws HoldingsItemsException {
        return new EnqueueService(em.unwrap(Connection.class), trackingId);
    }
}
