package dk.dbc.holdingsitems.jpa;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Entity
@Table(name = "supersedes")
@NamedQueries({
    @NamedQuery(name = SupersedesEntity.BY_SUPERSEDING, query = "SELECT s FROM SupersedesEntity s WHERE s.superseding = :superseding")
})
public class SupersedesEntity implements Serializable {

    private static final long serialVersionUID = 0x2B5BB72BD9F1ED0EL;

    public static final String BY_SUPERSEDING = "SupersedesEntity.bySuperseding";

    @Id
    @Column(name = "superseded", updatable = false)
    private String superseded;

    @Column(name = "superseding", updatable = true)
    private String superseding;

    public static List<SupersedesEntity> bySuperseding(EntityManager em, String superseding) {
        return em.createNamedQuery(BY_SUPERSEDING, SupersedesEntity.class)
                .setParameter("superseding", superseding)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    public static Stream<SupersedesEntity> bySupersedingNoLock(EntityManager em, String superseding) {
        return em.createNamedQuery(BY_SUPERSEDING, SupersedesEntity.class)
                .setParameter("superseding", superseding)
                .setLockMode(LockModeType.NONE)
                .getResultStream();
    }

    public SupersedesEntity() {
    }

    public SupersedesEntity(String superseded, String superseding) {
        this.superseded = superseded;
        this.superseding = superseding;
    }

    public String getSuperseded() {
        return superseded;
    }

    public void setSuperseded(String superseded) {
        this.superseded = superseded;
    }

    public String getSuperseding() {
        return superseding;
    }

    public void setSuperseding(String superseding) {
        this.superseding = superseding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(superseded, superseding);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final SupersedesEntity other = (SupersedesEntity) obj;
        return Objects.equals(this.superseded, other.superseded) &&
               Objects.equals(this.superseding, other.superseding);
    }

    @Override
    public String toString() {
        return "SupersedesEntity{" + "superseded=" + superseded + ", superseding=" + superseding + '}';
    }
}
