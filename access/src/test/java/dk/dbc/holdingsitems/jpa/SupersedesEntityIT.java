package dk.dbc.holdingsitems.jpa;

import dk.dbc.holdingsitems.JpaBase;
import java.util.List;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class SupersedesEntityIT extends JpaBase {

    @Test(timeout = 2_000L)
    public void testSupersedes() throws Exception {
        System.out.println("testSupersedes");

        jpa(em -> {
            SupersedesEntity entity = new SupersedesEntity("old", "new");
            em.persist(entity);
        });

        jpa(em -> {
            List<SupersedesEntity> entities = SupersedesEntity.byOwner(em, "new");
            assertThat(entities.size(), is(1));
            SupersedesEntity entity = entities.get(0);
            assertThat(entity.getSuperseded(), is("old"));
        });
    }
}
