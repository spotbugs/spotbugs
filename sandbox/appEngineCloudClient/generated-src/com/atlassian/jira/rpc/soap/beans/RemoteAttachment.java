/**
 * RemoteAttachment.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.atlassian.jira.rpc.soap.beans;

public class RemoteAttachment  extends com.atlassian.jira.rpc.soap.beans.AbstractRemoteEntity  implements java.io.Serializable {
    private java.lang.String author;

    private java.util.Calendar created;

    private java.lang.String filename;

    private java.lang.Long filesize;

    private java.lang.String mimetype;

    public RemoteAttachment() {
    }

    public RemoteAttachment(
           java.lang.String id,
           java.lang.String author,
           java.util.Calendar created,
           java.lang.String filename,
           java.lang.Long filesize,
           java.lang.String mimetype) {
        super(
            id);
        this.author = author;
        this.created = created;
        this.filename = filename;
        this.filesize = filesize;
        this.mimetype = mimetype;
    }


    /**
     * Gets the author value for this RemoteAttachment.
     * 
     * @return author
     */
    public java.lang.String getAuthor() {
        return author;
    }


    /**
     * Sets the author value for this RemoteAttachment.
     * 
     * @param author
     */
    public void setAuthor(java.lang.String author) {
        this.author = author;
    }


    /**
     * Gets the created value for this RemoteAttachment.
     * 
     * @return created
     */
    public java.util.Calendar getCreated() {
        return created;
    }


    /**
     * Sets the created value for this RemoteAttachment.
     * 
     * @param created
     */
    public void setCreated(java.util.Calendar created) {
        this.created = created;
    }


    /**
     * Gets the filename value for this RemoteAttachment.
     * 
     * @return filename
     */
    public java.lang.String getFilename() {
        return filename;
    }


    /**
     * Sets the filename value for this RemoteAttachment.
     * 
     * @param filename
     */
    public void setFilename(java.lang.String filename) {
        this.filename = filename;
    }


    /**
     * Gets the filesize value for this RemoteAttachment.
     * 
     * @return filesize
     */
    public java.lang.Long getFilesize() {
        return filesize;
    }


    /**
     * Sets the filesize value for this RemoteAttachment.
     * 
     * @param filesize
     */
    public void setFilesize(java.lang.Long filesize) {
        this.filesize = filesize;
    }


    /**
     * Gets the mimetype value for this RemoteAttachment.
     * 
     * @return mimetype
     */
    public java.lang.String getMimetype() {
        return mimetype;
    }


    /**
     * Sets the mimetype value for this RemoteAttachment.
     * 
     * @param mimetype
     */
    public void setMimetype(java.lang.String mimetype) {
        this.mimetype = mimetype;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RemoteAttachment)) return false;
        RemoteAttachment other = (RemoteAttachment) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = super.equals(obj) && 
            ((this.author==null && other.getAuthor()==null) || 
             (this.author!=null &&
              this.author.equals(other.getAuthor()))) &&
            ((this.created==null && other.getCreated()==null) || 
             (this.created!=null &&
              this.created.equals(other.getCreated()))) &&
            ((this.filename==null && other.getFilename()==null) || 
             (this.filename!=null &&
              this.filename.equals(other.getFilename()))) &&
            ((this.filesize==null && other.getFilesize()==null) || 
             (this.filesize!=null &&
              this.filesize.equals(other.getFilesize()))) &&
            ((this.mimetype==null && other.getMimetype()==null) || 
             (this.mimetype!=null &&
              this.mimetype.equals(other.getMimetype())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = super.hashCode();
        if (getAuthor() != null) {
            _hashCode += getAuthor().hashCode();
        }
        if (getCreated() != null) {
            _hashCode += getCreated().hashCode();
        }
        if (getFilename() != null) {
            _hashCode += getFilename().hashCode();
        }
        if (getFilesize() != null) {
            _hashCode += getFilesize().hashCode();
        }
        if (getMimetype() != null) {
            _hashCode += getMimetype().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(RemoteAttachment.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://beans.soap.rpc.jira.atlassian.com", "RemoteAttachment"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("author");
        elemField.setXmlName(new javax.xml.namespace.QName("", "author"));
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
        elemField.setFieldName("filename");
        elemField.setXmlName(new javax.xml.namespace.QName("", "filename"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("filesize");
        elemField.setXmlName(new javax.xml.namespace.QName("", "filesize"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mimetype");
        elemField.setXmlName(new javax.xml.namespace.QName("", "mimetype"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
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
