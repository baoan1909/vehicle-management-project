package com.ban.vehicle_management.application.storage.port.out;

import com.ban.vehicle_management.application.storage.model.StoreFileCommand;
import com.ban.vehicle_management.application.storage.model.StoredFile;

public interface FileStoragePort {

    StoredFile store(StoreFileCommand command);

    void delete(String objectKey);

    boolean exists(String objectKey);
}
