package dk.dbc.holdingsitems.content;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.holdingsitems.jpa.BibliographicItemEntity;
import dk.dbc.holdingsitems.jpa.IssueEntity;
import dk.dbc.holdingsitems.jpa.Status;
import java.time.Instant;
import java.time.LocalDate;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ServiceIT extends AbstractITBase {

    @Test(timeout = 2_000L)
    public void testLastLoanDateExistsInComplete() throws Exception {
        System.out.println("testLastLoanDateExistsInComplete");

        jpa(em -> {
            BibliographicItemEntity bibliographicItemEntity = BibliographicItemEntity.from(em, 710100, "25912233", Instant.now(), LocalDate.now())
                    .setTrackingId("x");

            IssueEntity issueEntity = issueEntity(bibliographicItemEntity, "issue#1")
                    .setIssueText("First Issue")
                    .setReadyForLoan(1)
                    .setTrackingId("x");

            itemEntity(issueEntity, "a", Status.ON_SHELF);

            bibliographicItemEntity.save();
        });
        JsonNode complete = get(API_URI.path("complete").path("710100").path("25912233").build())
                .statusIs(Response.Status.OK)
                .as(JsonNode.class);
        assertThat(complete.isMissingNode(), is(false));
    }

}
