/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.model.db;

/**
 *
 * @author belmann
 */
import java.io.File;
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
    @Column(name = "reg_name")
    private String name;
    @ManyToOne
    @JoinColumn(name = "reference_id", nullable = false)
    private Reference reference;
    @Basic
    @NotNull
    @Column(name = "reg_length")
    private int length;
    @Basic
    @NotNull
    @Column(name = "reg_start")
    private int start;
    @Basic
    @NotNull
    @Column(name = "reg_stop")
    private int stop;

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
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
