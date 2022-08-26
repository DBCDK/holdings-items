package dk.dbc.holdingsitems.jpa;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
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
    @NamedQuery(name = SupersedesEntity.BY_OWNER, query = "SELECT s FROM SupersedesEntity s WHERE s.owner = :owner")
})
public class SupersedesEntity implements Serializable {

    private static final long serialVersionUID = 0x2B5BB72BD9F1ED0EL;

    public static final String BY_OWNER = "SupersedesEntity.byOwner";

    @Id
    @Column(name = "overtaken", updatable = false)
    private String overtaken;

    @Column(name = "owner", updatable = true)
    private String owner;

    public static List<SupersedesEntity> byOwner(EntityManager em, String owner) {
        return em.createNamedQuery(BY_OWNER, SupersedesEntity.class)
                .setParameter("owner", owner)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }

    public SupersedesEntity() {
    }

    public SupersedesEntity(String overtaken, String owner) {
        this.overtaken = overtaken;
        this.owner = owner;
    }

    public String getOvertaken() {
        return overtaken;
    }

    public void setOvertaken(String overtaken) {
        this.overtaken = overtaken;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(overtaken, owner);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final SupersedesEntity other = (SupersedesEntity) obj;
        return Objects.equals(this.overtaken, other.overtaken) &&
               Objects.equals(this.owner, other.owner);
    }

    @Override
    public String toString() {
        return "SupersedesEntity{" + "overtaken=" + overtaken + ", owner=" + owner + '}';
    }
}
