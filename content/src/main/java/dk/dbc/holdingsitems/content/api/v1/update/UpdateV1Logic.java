package dk.dbc.holdingsitems.content.api.v1.update;

import dk.dbc.holdingsitems.EnqueueService;
import dk.dbc.holdingsitems.HoldingsItemsDAO;
import dk.dbc.holdingsitems.HoldingsItemsException;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.ItemEntity;
import dk.dbc.holdingsitems.jpa.LoanRestriction;
import dk.dbc.holdingsitems.jpa.Status;
import dk.dbc.oss.ns.holdingsitemsupdate.BibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.CompleteHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.Holding;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItem;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.HoldingsItemsUpdateStatusEnum;
import dk.dbc.oss.ns.holdingsitemsupdate.ModificationTimeStamp;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineBibliographicItem;
import dk.dbc.oss.ns.holdingsitemsupdate.OnlineHoldingsItemsUpdateRequest;
import dk.dbc.oss.ns.holdingsitemsupdate.StatusType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dbc.oss.ns.holdingsitemsupdate.StatusType.DECOMMISSIONED;
import static dk.dbc.oss.ns.holdingsitemsupdate.StatusType.ONLINE;

public class UpdateV1Logic {

    private static final Logger log = LoggerFactory.getLogger(UpdateV1Logic.class);

    private static final Comparator<BibliographicItem> BIBLIOGRAPHICITEM_SORT_COMPARE =
            Comparator.comparing(BibliographicItem::getBibliographicRecordId);
    private static final Comparator<OnlineBibliographicItem> ONLINE_BIBLIOGRAPHICITEM_SORT_COMPARE =
            Comparator.comparing(OnlineBibliographicItem::getBibliographicRecordId);

    @Inject
    EntityManager em;

    @Inject
    @ConfigProperty(name = "COMPLETE_SUPPLIER", defaultValue = "COMPLETE")
    String completeSupplier;

    @Inject
    @ConfigProperty(name = "COMPLETE_ORIGINAL_SUPPLIER", defaultValue = "COMPLETE_ORIGINAL")
    String completeOriginalSupplier;

    @Inject
    @ConfigProperty(name = "ONLINE_SUPPLIER", defaultValue = "ONLINE")
    String onlineSupplier;

    @Inject
    @ConfigProperty(name = "ONLINE_ORIGINAL_SUPPLIER", defaultValue = "ONLINE_ORIGINAL")
    String onlineOriginalSupplier;

    @Inject
    @ConfigProperty(name = "UPDATE_SUPPLIER", defaultValue = "UPDATE")
    String updateSupplier;

    @Inject
    @ConfigProperty(name = "UPDATE_ORIGINAL_SUPPLIER", defaultValue = "UPDATE_ORIGINAL")
    String updateOriginalSupplier;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void ensureRoot(int agencyId, String bibliographicRecordId) {
        BibliographicItemEntity entity = BibliographicItemEntity.from(em, agencyId, bibliographicRecordId,
                                                                      Instant.EPOCH, LocalDate.EPOCH);
        if (entity.isNew()) {
            entity.setTrackingId("");
            entity.save();
        }
    }

    public void complete(CompleteHoldingsItemsUpdateRequest req) throws HoldingsItemsException, UpdateException {
        String trackingId = req.getTrackingId();
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        try (EnqueueService queue = dao.enqueueService()) {
            CompleteBibliographicItem bibliographic = req.getCompleteBibliographicItem();
            log.debug("complete {}/{}", req.getAgencyId(), bibliographic.getBibliographicRecordId());
            Instant modified = modified(bibliographic.getModificationTimeStamp());
            Predicate<Instant> canChange = makeCanChange(modified);
            BibliographicItemEntity root = BibliographicItemEntity.from(em, req.getAgencyId(), bibliographic.getBibliographicRecordId(),
                                                                        Instant.EPOCH, LocalDate.EPOCH);
            LibIntChanges libIntChanges = new LibIntChanges(root);
            if (canChange.test(root.getModified()) || root.isNew()) {
                root.setModified(modified);
                root.setNote(bibliographic.getNote() == null ? "" : bibliographic.getNote());
                root.setTrackingId(trackingId);
            }

            for (Holding holding : bibliographic.getHolding()) {

                IssueEntity issue = root.issue(holding.getIssueId(), modified);
                if (updateIssue(holding, issue, modified, trackingId)) {
                    issue.setComplete(modified);
                }
                for (HoldingsItem holdingsItem : holding.getHoldingsItem()) {
                    ItemEntity item = issue.item(holdingsItem.getItemId(), modified);
                    switch (holdingsItem.getStatus()) {
                        case ONLINE:
                            throw new UpdateException(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "You cannot set ONLINE status with update, use online endpoint");
                        case DECOMMISSIONED:
                            issue.removeItem(item);
                            break;
                        default:
                            updateItem(holdingsItem, item, modified, trackingId);
                            break;
                    }
                }
            }

            log.trace("Cleaning up abandoned items");
            List<IssueEntity> removeIssues = root.stream()
                    .map(issue -> {
                        List<ItemEntity> removeItems = issue.stream()
                                .filter(item -> item.getModified().isBefore(modified))
                                .filter(item -> item.getStatus() != Status.ONLINE) // Complete does not change Online
                                .collect(Collectors.toList());
                        log.trace("removeItems = {}", removeItems);
                        removeItems.forEach(issue::removeItem);
                        return issue;
                    })
                    .filter(IssueEntity::isEmpty)
                    .collect(Collectors.toList());
            log.trace("removeIssues = {}", removeIssues);
            removeIssues.forEach(root::removeIssue);

            root.save();
            String jsonReport = libIntChanges.report(true);
            queue.enqueue(completeOriginalSupplier, req.getAgencyId(), bibliographic.getBibliographicRecordId(), jsonReport);
            queue.enqueue(completeSupplier, req.getAgencyId(), dao.getActualBibliographicRecordId(bibliographic.getBibliographicRecordId()), jsonReport);
        }
    }

    public void update(HoldingsItemsUpdateRequest req) throws HoldingsItemsException, UpdateException {
        String trackingId = req.getTrackingId();
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        try (EnqueueService queue = dao.enqueueService()) {
            List<BibliographicItem> bibliographicItems = req.getBibliographicItem().stream()
                    .sorted(BIBLIOGRAPHICITEM_SORT_COMPARE)
                    .collect(Collectors.toList());

            for (BibliographicItem bibliographic : bibliographicItems) {
                log.debug("update {}/{}", req.getAgencyId(), bibliographic.getBibliographicRecordId());
                Instant modified = modified(bibliographic.getModificationTimeStamp());
                Predicate<Instant> canChange = makeCanChange(modified);
                BibliographicItemEntity root = BibliographicItemEntity.from(em, req.getAgencyId(), bibliographic.getBibliographicRecordId(),
                                                                            Instant.EPOCH, LocalDate.EPOCH);
                LibIntChanges libIntChanges = new LibIntChanges(root);
                if (canChange.test(root.getModified()) || root.isNew()) {
                    root.setModified(modified);
                    root.setNote(bibliographic.getNote() == null ? "" : bibliographic.getNote());
                    root.setTrackingId(trackingId);
                }

                for (Holding holding : bibliographic.getHolding()) {
                    IssueEntity issue = root.issue(holding.getIssueId(), modified);
                    updateIssue(holding, issue, modified, trackingId);
                    for (HoldingsItem holdingsItem : holding.getHoldingsItem()) {
                        if (hasNewerItem(root, holding.getIssueId(), holdingsItem.getItemId(), modified))
                            continue;
                        ItemEntity item = issue.item(holdingsItem.getItemId(), modified);
                        switch (holdingsItem.getStatus()) {
                            case ONLINE:
                                throw new UpdateException(HoldingsItemsUpdateStatusEnum.FAILED_UPDATE_INTERNAL_ERROR, "You cannot set ONLINE status with update, use online endpoint");
                            case DECOMMISSIONED:
                                issue.removeItem(item);
                                break;
                            default:
                                updateItem(holdingsItem, item, modified, trackingId);
                                break;
                        }
                    }
                    if (issue.isEmpty()) {
                        root.removeIssue(issue);
                    }
                }
                root.save();
                String jsonReport = libIntChanges.report(false);
                queue.enqueue(updateOriginalSupplier, req.getAgencyId(), bibliographic.getBibliographicRecordId(), jsonReport);
                queue.enqueue(updateSupplier, req.getAgencyId(), dao.getActualBibliographicRecordId(bibliographic.getBibliographicRecordId()), jsonReport);
            }
        }
    }

    public void online(OnlineHoldingsItemsUpdateRequest req) throws HoldingsItemsException {
        String trackingId = req.getTrackingId();
        HoldingsItemsDAO dao = HoldingsItemsDAO.newInstance(em, trackingId);
        try (EnqueueService queue = dao.enqueueService()) {
            List<OnlineBibliographicItem> bibliographicItems = req.getOnlineBibliographicItem().stream()
                    .sorted(ONLINE_BIBLIOGRAPHICITEM_SORT_COMPARE)
                    .collect(Collectors.toList());
            for (OnlineBibliographicItem bibliographic : bibliographicItems) {
                log.debug("online {}/{}", req.getAgencyId(), bibliographic.getBibliographicRecordId());
                Instant modified = modified(bibliographic.getModificationTimeStamp());
                Predicate<Instant> canChange = makeCanChange(modified);
                BibliographicItemEntity root = BibliographicItemEntity.from(em, req.getAgencyId(), bibliographic.getBibliographicRecordId(),
                                                                            Instant.EPOCH, LocalDate.EPOCH);
                LibIntChanges libIntChanges = new LibIntChanges(root);
                IssueEntity issue = root.issue("", modified);

                if (bibliographic.isHasOnlineHolding()) {
                    if (canChange.test(issue.getModified()) || issue.isNew()) {
                        issue.setIssueText("ONLINE");
                        issue.setModified(modified);
                        ItemEntity item = issue.item("", modified);
                        if (canChange.test(item.getModified()) || item.isNew()) {
                            item.setStatus(Status.ONLINE);
                            item.setModified(modified);
                            item.setBranch("");
                            item.setBranchId("");
                            item.setDepartment("");
                            item.setLocation("");
                            item.setSubLocation("");
                            item.setCirculationRule("");
                            item.setAccessionDate(LocalDate.now());
                            item.setLastLoanDate(null);
                            item.setTrackingId(trackingId);
                        }
                    }
                } else if (canChange.test(issue.getModified())) {
                    root.removeIssue(issue);
                }
                root.save();
                String jsonReport = libIntChanges.report(false);
                queue.enqueue(onlineOriginalSupplier, req.getAgencyId(), bibliographic.getBibliographicRecordId(), jsonReport);
                queue.enqueue(onlineSupplier, req.getAgencyId(), dao.getActualBibliographicRecordId(bibliographic.getBibliographicRecordId()), jsonReport);
            }
        }
    }

    private boolean updateIssue(Holding holding, IssueEntity issue, Instant modified, String trackingId) {
        Predicate<Instant> canChange = makeCanChange(modified);
        boolean changed = canChange.test(issue.getModified()) || issue.isNew();
        if (changed) {
            issue.setModified(modified);
            issue.setExpectedDelivery(toLocalDate(holding.getExpectedDeliveryDate()));
            issue.setIssueText(holding.getIssueText());
            issue.setReadyForLoan((int) (long) holding.getReadyForLoan());
            issue.setTrackingId(trackingId);
        }
        return changed;
    }

    private boolean updateItem(HoldingsItem holdingsitem, ItemEntity item, Instant modified, String trackingId) {
        Predicate<Instant> canChange = makeCanChange(modified);
        boolean changed = canChange.test(item.getModified()) || item.isNew();
        if (changed) {
            LocalDate accessionDate = toLocalDate(holdingsitem.getAccessionDate());
            item.setAccessionDate(accessionDate);
            item.setBranch(holdingsitem.getBranch());
            item.setBranchId(holdingsitem.getBranchId());
            item.setCirculationRule(holdingsitem.getCirculationRule());
            item.setDepartment(holdingsitem.getDepartment());
            item.setLastLoanDate(toLocalDate(holdingsitem.getLastLoanDate()));
            item.setLoanRestriction(LoanRestriction.parse(holdingsitem.getLoanRestriction()));
            item.setLocation(holdingsitem.getLocation());
            item.setModified(modified);
            item.setStatus(convert(holdingsitem.getStatus()));
            item.setSubLocation(holdingsitem.getSubLocation());
            item.setTrackingId(trackingId);
        }
        return changed;
    }

    private boolean hasNewerItem(BibliographicItemEntity root, String issueId, String itemId, Instant modified) {
        Iterator<ItemEntity> iterator = root.stream()
                .flatMap(IssueEntity::stream)
                .filter(i -> i.getStatus() != Status.ONLINE)
                .filter(i -> itemId.equals(i.getItemId()))
                .sorted(Comparator.comparing(ItemEntity::getModified).reversed())
                .iterator();
        // has no items with that id
        if (!iterator.hasNext())
            return false;
        ItemEntity first = iterator.next();
        boolean firstIsOlder = first.getModified().isBefore(modified);

        // remove all extra (db cleanup)
        while (iterator.hasNext()) {
            ItemEntity removableItem = iterator.next();
            if (firstIsOlder && removableItem.getIssueId().equals(issueId)) // Keep this to be modified
                continue;
            IssueEntity removableIssue = root.issue(removableItem.getIssueId(), modified);
            removableIssue.removeItem(removableItem);
            if (removableIssue.isEmpty()) {
                root.removeIssue(removableIssue);
            }
        }

        if (!firstIsOlder)
            return true;

        // remove if it is old and not the one we want to modify
        if (!first.getIssueId().equals(issueId)) {
            IssueEntity removableIssue = root.issue(first.getIssueId(), modified);
            removableIssue.removeItem(first);
            if (removableIssue.isEmpty()) {
                root.removeIssue(removableIssue);
            }
        }

        return false;
    }

    private static Status convert(StatusType status) {
        if (status == null)
            return null;
        switch (status) {
            case DECOMMISSIONED:
                return Status.DECOMMISSIONED;
            case DISCARDED:
                return Status.DISCARDED;
            case LOST:
                return Status.LOST;
            case NOT_FOR_LOAN:
                return Status.NOT_FOR_LOAN;
            case ONLINE:
                return Status.ONLINE;
            case ON_LOAN:
                return Status.ON_LOAN;
            case ON_ORDER:
                return Status.ON_ORDER;
            case ON_SHELF:
                return Status.ON_SHELF;
            default:
                throw new AssertionError();
        }
    }

    private static Instant modified(ModificationTimeStamp ts) {
        return ts.getModificationDateTime().plusMillis(ts.getModificationMilliSeconds());
    }

    private static Predicate<Instant> makeCanChange(Instant modified) {
        return dbTime -> !dbTime.isAfter(modified);
    }

    private static LocalDate toLocalDate(Instant instant) {
        if (instant == null)
            return null;
        return LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }
}
