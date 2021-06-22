/*
 * Copyright (C) 2021 DBC A/S (http://dbc.dk/)
 *
 * This is part of holdings-items-content-service
 *
 * holdings-items-content-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * holdings-items-content-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.holdingsitems.content.response;

import dk.dbc.holdingsitems.jpa.ItemEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class CompleteItem {

    public String itemId;
    public String branchId;
    public String branch;
    public String department;
    public String location;
    public String subLocation;
    public String circulationRule;
    public String loanRestriction;
    public LocalDate accessionDate;
    public String status;

    public CompleteItem() {
    }

    public CompleteItem(ItemEntity item) {
        this.itemId = item.getItemId();
        this.branchId = item.getBranchId();
        this.branch = item.getBranch();
        this.department = item.getDepartment();
        this.loanRestriction = item.getLoanRestriction().toString();
        this.location = item.getLocation();
        this.subLocation = item.getSubLocation();
        this.circulationRule = item.getCirculationRule();
        this.loanRestriction = item.getLoanRestriction().toString();
        this.accessionDate = item.getAccessionDate();
        this.status = item.getStatus().toString();
    }

    @Override
    public String toString() {
        return "CompleteItem{" + "itemId=" + itemId + ", branchId=" + branchId + ", branch=" + branch + ", department=" + department + ", location=" + location + ", subLocation=" + subLocation + ", circulationRule=" + circulationRule + ", loanRestriction=" + loanRestriction + ", accessionDate=" + accessionDate + ", status=" + status + '}';
    }

}
