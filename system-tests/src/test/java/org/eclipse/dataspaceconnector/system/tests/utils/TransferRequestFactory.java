package org.eclipse.dataspaceconnector.system.tests.utils;

import java.util.function.Function;

public interface TransferRequestFactory extends Function<TransferSimulationUtils.TransferInitiationData, String> {
}
