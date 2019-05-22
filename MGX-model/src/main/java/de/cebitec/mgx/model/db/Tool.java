package de.cebitec.mgx.model.db;


/**
 *
 * @author sjaenick
 */
public class Tool extends Identifiable {

    private static final long serialVersionUID = 1L;
    protected String name;
    protected String description;
    protected Float version;
    protected String author;
    protected String url;
    protected String file;
    protected String scope;

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

    public String getFile() {
        return file;
    }

    public Tool setFile(String file) {
        this.file = file;
        return this;
    }

    // either 'read' or 'assembly'
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (getId() != INVALID_IDENTIFIER ? Long.valueOf(getId()).hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Tool)) {
            return false;
        }
        Tool other = (Tool) object;
        return !((this.getId() == INVALID_IDENTIFIER && other.getId() != INVALID_IDENTIFIER) 
                || (this.getId() != INVALID_IDENTIFIER && this.getId() != other.getId()));
    }
}
