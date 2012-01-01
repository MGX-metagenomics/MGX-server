package de.cebitec.mgx.model.db;

import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 *
 * @author sjaenick
 */
@Entity
@Table(name = "Tool")
public class Tool implements Serializable, Identifiable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Basic
    @NotNull
    @Column(name = "name")
    protected String name;
    @Basic
    @NotNull
    @Column(name = "description")
    protected String description;
    @Basic
    @NotNull
    @Column(name = "version")
    protected Float version;
    @Basic
    @NotNull
    @Column(name = "author")
    protected String author;
    @Basic
    @Column(name = "url")
    protected String url;
    @Basic
    @Column(name = "xml_file")
    protected String xml_file;

    @Override
    public Long getId() {
        return id;
    }

    public Tool setId(Long id) {
        this.id = id;
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public Tool setAuthor(String author) {
        this.author = author;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Tool setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public Tool setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Tool setUrl(String url) {
        this.url = url;
        return this;
    }

    public Float getVersion() {
        return version;
    }

    public Tool setVersion(Float version) {
        this.version = version;
        return this;
    }

    public String getXMLFile() {
        return xml_file;
    }

    public Tool setXMLFile(String xml_file) {
        this.xml_file = xml_file;
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Tool)) {
            return false;
        }
        Tool other = (Tool) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.cebitec.mgx.model.db.Tool[id=" + id + "]";
    }
}
