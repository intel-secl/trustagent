/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.02.24 at 02:22:36 PM PST 
//


package com.intel.mountwilson.ta.data.quoterequest;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="nonce" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="pcr_list" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "nonce",
    "pcrList"
})
@XmlRootElement(name = "quote_request")
public class QuoteRequest {

    @XmlElement(required = true)
    protected String nonce;
    @XmlElement(name = "pcr_list", required = true)
    protected String pcrList;

    /**
     * Gets the value of the nonce property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * Sets the value of the nonce property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNonce(String value) {
        this.nonce = value;
    }

    /**
     * Gets the value of the pcrList property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPcrList() {
        return pcrList;
    }

    /**
     * Sets the value of the pcrList property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPcrList(String value) {
        this.pcrList = value;
    }

}
