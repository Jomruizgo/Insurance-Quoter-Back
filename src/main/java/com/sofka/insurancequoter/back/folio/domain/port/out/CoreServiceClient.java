package com.sofka.insurancequoter.back.folio.domain.port.out;

// Output port — contract for calling the core service (Insurance-Quoter-Core)
public interface CoreServiceClient {
    boolean existsSubscriber(String subscriberId);
    boolean existsAgent(String agentCode);
    String nextFolioNumber();
}
