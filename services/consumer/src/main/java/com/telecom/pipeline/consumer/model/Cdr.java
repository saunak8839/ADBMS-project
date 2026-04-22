package com.telecom.pipeline.consumer.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "cdrs")
@IdClass(CdrCompositeKey.class)
public class Cdr {
    @Id
    private UUID id;
    
    @Id
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "caller_number")
    private String callerNumber;

    @Column(name = "receiver_number")
    private String receiverNumber;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "cell_id")
    private Integer cellId;

    @Column(name = "call_type")
    private String callType;

    private String status;
    private BigDecimal cost;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public String getCallerNumber() { return callerNumber; }
    public void setCallerNumber(String callerNumber) { this.callerNumber = callerNumber; }

    public String getReceiverNumber() { return receiverNumber; }
    public void setReceiverNumber(String receiverNumber) { this.receiverNumber = receiverNumber; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getCellId() { return cellId; }
    public void setCellId(Integer cellId) { this.cellId = cellId; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
}
