package com.mesosphere.sdk.operator.scheduler;

import com.mesosphere.sdk.state.ConfigStoreException;
import com.mesosphere.sdk.state.ConfigTargetStore;

import java.util.UUID;

public class BasicConfigTargetStore implements ConfigTargetStore {
    private UUID uuid;

    BasicConfigTargetStore() {
        uuid = UUID.randomUUID();
    }

    @Override
    public UUID getTargetConfig() throws ConfigStoreException {
        return uuid;
    }

    @Override
    public void setTargetConfig(UUID id) throws ConfigStoreException {
        this.uuid = id;
    }
}
