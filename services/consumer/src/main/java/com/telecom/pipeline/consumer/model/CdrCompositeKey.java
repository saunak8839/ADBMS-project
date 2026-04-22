package com.telecom.pipeline.consumer.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class CdrCompositeKey implements Serializable {
    private UUID id;
    private LocalDateTime startTime;

    public CdrCompositeKey() {}

    public CdrCompositeKey(UUID id, LocalDateTime startTime) {
        this.id = id;
        this.startTime = startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CdrCompositeKey that = (CdrCompositeKey) o;
        return Objects.equals(id, that.id) && Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startTime);
    }
}
