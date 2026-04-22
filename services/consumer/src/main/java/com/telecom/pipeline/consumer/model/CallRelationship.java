package com.telecom.pipeline.consumer.model;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDateTime;

@RelationshipProperties
public class CallRelationship {

    @RelationshipId
    private Long id;

    @Property("durationSec")
    private Integer durationSec;

    @Property("startTime")
    private LocalDateTime startTime;

    @Property("cdrId")
    private String cdrId;

    @TargetNode
    private SubscriberNode receiver;

    public CallRelationship() {
    }

    public CallRelationship(SubscriberNode receiver, String cdrId, LocalDateTime startTime, Integer durationSec) {
        this.receiver = receiver;
        this.cdrId = cdrId;
        this.startTime = startTime;
        this.durationSec = durationSec;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(Integer durationSec) {
        this.durationSec = durationSec;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getCdrId() {
        return cdrId;
    }

    public void setCdrId(String cdrId) {
        this.cdrId = cdrId;
    }

    public SubscriberNode getReceiver() {
        return receiver;
    }

    public void setReceiver(SubscriberNode receiver) {
        this.receiver = receiver;
    }
}
