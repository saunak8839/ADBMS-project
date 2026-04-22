package com.telecom.pipeline.producer.model;

import java.util.UUID;

public class CdrEvent {
    private UUID cdrId;
    private Long callerMsisdn;
    private Long calleeMsisdn;
    private String startTime;
    private Integer durationSec;
    private Integer cellId;

    public CdrEvent() {}

    public CdrEvent(UUID cdrId, Long callerMsisdn, Long calleeMsisdn, String startTime, Integer durationSec, Integer cellId) {
        this.cdrId = cdrId;
        this.callerMsisdn = callerMsisdn;
        this.calleeMsisdn = calleeMsisdn;
        this.startTime = startTime;
        this.durationSec = durationSec;
        this.cellId = cellId;
    }

    public UUID getCdrId() { return cdrId; }
    public void setCdrId(UUID cdrId) { this.cdrId = cdrId; }

    public Long getCallerMsisdn() { return callerMsisdn; }
    public void setCallerMsisdn(Long callerMsisdn) { this.callerMsisdn = callerMsisdn; }

    public Long getCalleeMsisdn() { return calleeMsisdn; }
    public void setCalleeMsisdn(Long calleeMsisdn) { this.calleeMsisdn = calleeMsisdn; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public Integer getDurationSec() { return durationSec; }
    public void setDurationSec(Integer durationSec) { this.durationSec = durationSec; }

    public Integer getCellId() { return cellId; }
    public void setCellId(Integer cellId) { this.cellId = cellId; }
}
