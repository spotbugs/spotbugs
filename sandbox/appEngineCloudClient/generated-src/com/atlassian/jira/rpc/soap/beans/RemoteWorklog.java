/**
 * RemoteWorklog.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.jira.rpc.soap.beans;

public class RemoteWorklog  implements java.io.Serializable {
    private java.lang.String author;

    private java.lang.String comment;

    private java.util.Calendar created;

    private java.lang.String groupLevel;

    private java.lang.String id;

    private java.lang.String roleLevelId;

    private java.util.Calendar startDate;

    private java.lang.String timeSpent;

    private long timeSpentInSeconds;

    private java.lang.String updateAuthor;

    private java.util.Calendar updated;

    public RemoteWorklog() {
    }

    public RemoteWorklog(
           java.lang.String author,
           java.lang.String comment,
           java.util.Calendar created,
           java.lang.String groupLevel,
           java.lang.String id,
           java.lang.String roleLevelId,
           java.util.Calendar startDate,
           java.lang.String timeSpent,
           long timeSpentInSeconds,
           java.lang.String updateAuthor,
           java.util.Calendar updated) {
           this.author = author;
           this.comment = comment;
           this.created = created;
           this.groupLevel = groupLevel;
           this.id = id;
           this.roleLevelId = roleLevelId;
           this.startDate = startDate;
           this.timeSpent = timeSpent;
           this.timeSpentInSeconds = timeSpentInSeconds;
           this.updateAuthor = updateAuthor;
           this.updated = updated;
    }


    /**
     * Gets the author value for this RemoteWorklog.
     * 
     * @return author
     */
    public java.lang.String getAuthor() {
        return author;
    }


    /**
     * Sets the author value for this RemoteWorklog.
     * 
     * @param author
     */
    public void setAuthor(java.lang.String author) {
        this.author = author;
    }


    /**
     * Gets the comment value for this RemoteWorklog.
     * 
     * @return comment
     */
    public java.lang.String getComment() {
        return comment;
    }


    /**
     * Sets the comment value for this RemoteWorklog.
     * 
     * @param comment
     */
    public void setComment(java.lang.String comment) {
        this.comment = comment;
    }


    /**
     * Gets the created value for this RemoteWorklog.
     * 
     * @return created
     */
    public java.util.Calendar getCreated() {
        return created;
    }


    /**
     * Sets the created value for this RemoteWorklog.
     * 
     * @param created
     */
    public void setCreated(java.util.Calendar created) {
        this.created = created;
    }


    /**
     * Gets the groupLevel value for this RemoteWorklog.
     * 
     * @return groupLevel
     */
    public java.lang.String getGroupLevel() {
        return groupLevel;
    }


    /**
     * Sets the groupLevel value for this RemoteWorklog.
     * 
     * @param groupLevel
     */
    public void setGroupLevel(java.lang.String groupLevel) {
        this.groupLevel = groupLevel;
    }


    /**
     * Gets the id value for this RemoteWorklog.
     * 
     * @return id
     */
    public java.lang.String getId() {
        return id;
    }


    /**
     * Sets the id value for this RemoteWorklog.
     * 
     * @param id
     */
    public void setId(java.lang.String id) {
        this.id = id;
    }


    /**
     * Gets the roleLevelId value for this RemoteWorklog.
     * 
     * @return roleLevelId
     */
    public java.lang.String getRoleLevelId() {
        return roleLevelId;
    }


    /**
     * Sets the roleLevelId value for this RemoteWorklog.
     * 
     * @param roleLevelId
     */
    public void setRoleLevelId(java.lang.String roleLevelId) {
        this.roleLevelId = roleLevelId;
    }


    /**
     * Gets the startDate value for this RemoteWorklog.
     * 
     * @return startDate
     */
    public java.util.Calendar getStartDate() {
        return startDate;
    }


    /**
     * Sets the startDate value for this RemoteWorklog.
     * 
     * @param startDate
     */
    public void setStartDate(java.util.Calendar startDate) {
        this.startDate = startDate;
    }


    /**
     * Gets the timeSpent value for this RemoteWorklog.
     * 
     * @return timeSpent
     */
    public java.lang.String getTimeSpent() {
        return timeSpent;
    }


    /**
     * Sets the timeSpent value for this RemoteWorklog.
     * 
     * @param timeSpent
     */
    public void setTimeSpent(java.lang.String timeSpent) {
        this.timeSpent = timeSpent;
    }


    /**
     * Gets the timeSpentInSeconds value for this RemoteWorklog.
     * 
     * @return timeSpentInSeconds
     */
    public long getTimeSpentInSeconds() {
        return timeSpentInSeconds;
    }


    /**
     * Sets the timeSpentInSeconds value for this RemoteWorklog.
     * 
     * @param timeSpentInSeconds
     */
    public void setTimeSpentInSeconds(long timeSpentInSeconds) {
        this.timeSpentInSeconds = timeSpentInSeconds;
    }


    /**
     * Gets the updateAuthor value for this RemoteWorklog.
     * 
     * @return updateAuthor
     */
    public java.lang.String getUpdateAuthor() {
        return updateAuthor;
    }


    /**
     * Sets the updateAuthor value for this RemoteWorklog.
     * 
     * @param updateAuthor
     */
    public void setUpdateAuthor(java.lang.String updateAuthor) {
        this.updateAuthor = updateAuthor;
    }


    /**
     * Gets the updated value for this RemoteWorklog.
     * 
     * @return updated
     */
    public java.util.Calendar getUpdated() {
        return updated;
    }


    /**
     * Sets the updated value for this RemoteWorklog.
     * 
     * @param updated
     */
    public void setUpdated(java.util.Calendar updated) {
        this.updated = updated;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RemoteWorklog)) return false;
        RemoteWorklog other = (RemoteWorklog) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.author==null && other.getAuthor()==null) || 
             (this.author!=null &&
              this.author.equals(other.getAuthor()))) &&
            ((this.comment==null && other.getComment()==null) || 
             (this.comment!=null &&
              this.comment.equals(other.getComment()))) &&
            ((this.created==null && other.getCreated()==null) || 
             (this.created!=null &&
              this.created.equals(other.getCreated()))) &&
            ((this.groupLevel==null && other.getGroupLevel()==null) || 
             (this.groupLevel!=null &&
              this.groupLevel.equals(other.getGroupLevel()))) &&
            ((this.id==null && other.getId()==null) || 
             (this.id!=null &&
              this.id.equals(other.getId()))) &&
            ((this.roleLevelId==null && other.getRoleLevelId()==null) || 
             (this.roleLevelId!=null &&
              this.roleLevelId.equals(other.getRoleLevelId()))) &&
            ((this.startDate==null && other.getStartDate()==null) || 
             (this.startDate!=null &&
              this.startDate.equals(other.getStartDate()))) &&
            ((this.timeSpent==null && other.getTimeSpent()==null) || 
             (this.timeSpent!=null &&
              this.timeSpent.equals(other.getTimeSpent()))) &&
            this.timeSpentInSeconds == other.getTimeSpentInSeconds() &&
            ((this.updateAuthor==null && other.getUpdateAuthor()==null) || 
             (this.updateAuthor!=null &&
              this.updateAuthor.equals(other.getUpdateAuthor()))) &&
            ((this.updated==null && other.getUpdated()==null) || 
             (this.updated!=null &&
              this.updated.equals(other.getUpdated())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getAuthor() != null) {
            _hashCode += getAuthor().hashCode();
        }
        if (getComment() != null) {
            _hashCode += getComment().hashCode();
        }
        if (getCreated() != null) {
            _hashCode += getCreated().hashCode();
        }
        if (getGroupLevel() != null) {
            _hashCode += getGroupLevel().hashCode();
        }
        if (getId() != null) {
            _hashCode += getId().hashCode();
        }
        if (getRoleLevelId() != null) {
            _hashCode += getRoleLevelId().hashCode();
        }
        if (getStartDate() != null) {
            _hashCode += getStartDate().hashCode();
        }
        if (getTimeSpent() != null) {
            _hashCode += getTimeSpent().hashCode();
        }
        _hashCode += new Long(getTimeSpentInSeconds()).hashCode();
        if (getUpdateAuthor() != null) {
            _hashCode += getUpdateAuthor().hashCode();
        }
        if (getUpdated() != null) {
            _hashCode += getUpdated().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(RemoteWorklog.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemoteWorklog"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("author");
        elemField.setXmlName(new javax.xml.namespace.QName("", "author"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("comment");
        elemField.setXmlName(new javax.xml.namespace.QName("", "comment"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("created");
        elemField.setXmlName(new javax.xml.namespace.QName("", "created"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("groupLevel");
        elemField.setXmlName(new javax.xml.namespace.QName("", "groupLevel"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("id");
        elemField.setXmlName(new javax.xml.namespace.QName("", "id"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("roleLevelId");
        elemField.setXmlName(new javax.xml.namespace.QName("", "roleLevelId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("startDate");
        elemField.setXmlName(new javax.xml.namespace.QName("", "startDate"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("timeSpent");
        elemField.setXmlName(new javax.xml.namespace.QName("", "timeSpent"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("timeSpentInSeconds");
        elemField.setXmlName(new javax.xml.namespace.QName("", "timeSpentInSeconds"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("updateAuthor");
        elemField.setXmlName(new javax.xml.namespace.QName("", "updateAuthor"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("updated");
        elemField.setXmlName(new javax.xml.namespace.QName("", "updated"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
