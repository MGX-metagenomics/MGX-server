package de.cebitec.mgx.model.db;

/**
 *
 * @author belmann
 */
import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author belmann
 */
@Entity
@Table(name = "Region")
public class Region implements Serializable, Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @NotNull
    @Column(name = "name")
    private String name;
    @Basic
    @NotNull
    @Column(name = "description")
    private String description;
    @Basic
    @NotNull
    @Column(name = "type")
    private String type;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_id", nullable = false)
    private Reference reference;
    @Basic
    @NotNull
    @Column(name = "reg_start")
    private int start;
    @Basic
    @NotNull
    @Column(name = "reg_stop")
    private int stop;

//    public Reference getReference() {
//        return reference;
//    }
//
//    public void setReference(Reference reference) {
//        this.reference = reference;
//    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Region";
    }
}
