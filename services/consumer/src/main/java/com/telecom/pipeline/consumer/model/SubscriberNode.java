package com.telecom.pipeline.consumer.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
@Node("Subscriber")
public class SubscriberNode {

    @Id
    private String phoneNumber;

    @Property("status")
    private String status;

    public SubscriberNode() {
    }

    public SubscriberNode(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.status = "ACTIVE";
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
