package com.telecom.pipeline.consumer.model;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Subscriber")
public class SubscriberNode {

    @Id
    private String phoneNumber;

    @Property("status")
    private String status;

    @Relationship(type = "CALLED", direction = Relationship.Direction.OUTGOING)
    private List<CallRelationship> calls = new ArrayList<>();

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

    public List<CallRelationship> getCalls() {
        return calls;
    }

    public void setCalls(List<CallRelationship> calls) {
        this.calls = calls;
    }

    public void addCall(CallRelationship call) {
        this.calls.add(call);
    }
}
