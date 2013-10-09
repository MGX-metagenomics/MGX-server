package de.cebitec.mgx.model.db;


import java.io.Serializable;
import java.util.Collection;
import javax.persistence.*;
import javax.validation.constraints.NotNull;


/**
 *
 * @author belmann
 */
@Entity
@Table(name = "Reference", uniqueConstraints =
@UniqueConstraint(columnNames = {"name"}))
public class Reference implements Serializable, Identifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToMany(mappedBy = "reference", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    private Collection<Region> regions;

    @Basic
    @NotNull
    @Column(name = "name")
    private String name;
    
    @Basic
    @NotNull
    @Column(name = "ref_length")
    private int length;
    
    @Basic
    @Column(name = "ref_filePath")
    private String filePath;
    
    
    public Collection<Region> getRegions() {
        return regions;
    }

    public void setRegions(Collection<Region> regions) {
        this.regions = regions;
    }
    
    public String getFile() {
        return filePath;
    }

    public void setFile(String filePath) {
        this.filePath = filePath;
    }
    
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Reference";
    }

    @Override
    public Long getId() {
     return id;   
    }
    
    public void setId(Long l) {
        id = l;
    }
}
