/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.02.24 at 02:22:35 PM PST 
//


package com.intel.mountwilson.ta.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for client_requestType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="client_requestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="clientIp" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="error_code" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="error_message" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="aikcert" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="quote" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "client_requestType", propOrder = {
    "timestamp",
    "clientIp",
    "errorCode",
    "errorMessage",
    "aikcert",
    "quote",
    "eventLog",
    "tcbMeasurement"
})
public class ClientRequestType {

    @XmlElement(required = true)
    protected String timestamp;
    @XmlElement(required = true)
    protected String clientIp;
    @XmlElement(name = "error_code")
    protected int errorCode;
    @XmlElement(name = "error_message", required = true)
    protected String errorMessage;
    @XmlElement(required = true)
    protected String aikcert;
    @XmlElement(required = true)
    protected String quote;
    @XmlElement(required = true)
    protected String eventLog; // This will have the list of modules from the event log, which we will parse later.
    @XmlElement(required = true)
    protected String tcbMeasurement; // This will have the list of additional modules including applications/data installed on top of OS.
    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTimestamp(String value) {
        this.timestamp = value;
    }

    /**
     * Gets the value of the clientIp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClientIp() {
        return clientIp;
    }

    /**
     * Sets the value of the clientIp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClientIp(String value) {
        this.clientIp = value;
    }

    /**
     * Gets the value of the errorCode property.
     * 
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the value of the errorCode property.
     * 
     */
    public void setErrorCode(int value) {
        this.errorCode = value;
    }

    /**
     * Gets the value of the errorMessage property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the value of the errorMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorMessage(String value) {
        this.errorMessage = value;
    }

    /**
     * Gets the value of the aikcert property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAikcert() {
        return aikcert;
    }

    /**
     * Sets the value of the aikcert property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAikcert(String value) {
        this.aikcert = value;
    }

    /**
     * Gets the value of the quote property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQuote() {
        return quote;
    }

    /**
     * Sets the value of the quote property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQuote(String value) {
        this.quote = value;
    }

    
    public String getEventLog() {
        return eventLog;
    }

    public void setEventLog(String eventLog) {
        this.eventLog = eventLog;
    }

    public String getTcbMeasurement() {
        return tcbMeasurement;
    }

    public void setTcbMeasurement(String tcbMeasurement) {
        this.tcbMeasurement = tcbMeasurement;
    }

    
}
