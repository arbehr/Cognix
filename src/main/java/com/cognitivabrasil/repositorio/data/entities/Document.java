package com.cognitivabrasil.repositorio.data.entities;

import ORG.oclc.oai.models.HibernateOaiDocument;
import cognitivabrasil.obaa.OBAA;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 *
 * @author Paulo Schreiner <paulo@jorjao81.com>
 * @author Marcos Nunes <marcosn@gmail.com>
 *
 */
@Entity
@Table(name = "documents")
public class Document implements HibernateOaiDocument, java.io.Serializable {

    private final Logger log = Logger.getLogger(Document.class);
    private Integer id;
    private String obaaEntry;
    @DateTimeFormat(style = "M-")
    private DateTime created;
    private Boolean deleted;
    private String obaaXml;
    private List<Files> files;
    private OBAA metadata;
    private User owner;

    public Document() {
        obaaEntry = "";
        deleted = false;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer getId() {
        return this.id;
    }

    protected void setId(Integer id) {
        this.id = id;
    }

    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
    public DateTime getCreated() {
        return this.created;
    }
    
    @Transient
    @Override
    public Date getTimestamp(){
        return this.created.toDate();
    }

    public void setCreated(DateTime date) {
        this.created = date;
    }

    /**
     * Format the created in this format: dd/MM/yyyy HH:mm:ss
     *
     * @return String whith the formatted created
     */
    @Transient
    public String getTimestampFormatted() {
        if (this.created == null) {
            return "";
        } else {
            return this.created.toString("dd/MM/yyyy HH:mm:ss");
        }
    }

    @Column(name="obaa_entry")
    public String getObaaEntry() {
        return this.obaaEntry;
    }

    public void setObaaEntry(String entry) {
        this.obaaEntry = entry;
    }

    /**
     * @return the excluido
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param excluido the excluido to set
     */
    public void setDeleted(Boolean excluido) {
        this.deleted = excluido;
    }

    public void isDeleted(boolean excluido) {
        this.deleted = excluido;
    }

    /**
     * Gets the OBAA XML directly, consider using getMetadata() instead.
     *
     * @return the obaaXml
     */
    public String getObaaXml() {
        return obaaXml;
    }

    /**
     * Sets the OBAA XML directly, consider using setMetadata instead.
     *
     * @param obaaXml the obaaXml to set
     */
    public void setObaaXml(String obaaXml) {
        this.obaaXml = obaaXml;
    }

    /**
     * Returns the document metadata.
     *
     * @return the metadata
     * @throws IllegalStateException if there is no XML metadata associated with
     * the document
     */
    @Transient
    public OBAA getMetadata() {
        if (metadata == null) {
            if (getObaaXml() == null) {
                metadata = new OBAA();
//				throw new IllegalStateException("No XML metadata associated with the Object");
            } else {
                metadata = OBAA.fromString(getObaaXml());
            }
        }
        return metadata;
    }

    /**
     * Sets the metadata of the object, and updates the corresponding XML.
     *
     * @param metadata the metadata to set
     */
    public void setMetadata(OBAA metadata) {
        this.metadata = metadata;
//		updateIndexes();
        try {
            setObaaXml(metadata.toXml());
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
        }
    }

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    public List<Files> getFiles() {
        return files;
    }

    public void setFiles(List<Files> files) {
        this.files = files;
    }

    public void addFile(Files f) {
        if (files == null) {
            files = new ArrayList<>();
        }
        files.add(f);
    }

    @Transient
    public String getTitle() {
        if (getObaaXml() == null || getObaaXml().isEmpty()) {
            return "Sem título";
        }
        try {
            return getMetadata().getGeneral().getTitles().get(0);
        } catch (NullPointerException e) {
            log.error(e);
            return "NullPointer";
        } catch (IndexOutOfBoundsException e) {
            log.error(e);
            return "IndexOutOfBounds";
        }
    }

    /**
     * Removes a file from the collection. (does NOT delete it from database)
     *
     * @param item
     */
    public void removeFile(Files item) {
        this.files.remove(item);
    }

    @Transient
    public String getXml() {
        return getObaaXml();
    }

    @Transient
    public String getOaiIdentifier() {
        return getObaaEntry();
    }

    @Transient
    public Collection<String> getSets() {
        // TODO: currently we return an empty set, should implement this functionality
        Collection<String> c = new HashSet<String>();
        return c;
    }

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

}