package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.domain.port.in.CreateFolioUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteRepository;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

// Orchestrates idempotency check → reference validation → folio generation → persistence
@RequiredArgsConstructor
public class CreateFolioUseCaseImpl implements CreateFolioUseCase {

    private final QuoteRepository quoteRepository;
    private final CoreServiceClient coreServiceClient;

    @Override
    public FolioCreationResult createFolio(CreateFolioCommand command) {
        // Step 1: idempotency — return existing CREATED folio if found
        Optional<Quote> existing =
                quoteRepository.findActiveBySubscriberAndAgent(command.subscriberId(), command.agentCode());
        if (existing.isPresent()) {
            return new FolioCreationResult(existing.get(), false);
        }

        // Step 2: validate subscriber reference
        if (!coreServiceClient.existsSubscriber(command.subscriberId())) {
            throw new InvalidReferenceException(
                    "Subscriber not found: " + command.subscriberId());
        }

        // Step 3: validate agent reference
        if (!coreServiceClient.existsAgent(command.agentCode())) {
            throw new InvalidReferenceException(
                    "Agent not found: " + command.agentCode());
        }

        // Step 4: obtain folio number from core service
        String folioNumber = coreServiceClient.nextFolioNumber();

        // Step 5: build new Quote and persist it
        Instant now = Instant.now();
        Quote newQuote = new Quote(
                folioNumber,
                QuoteStatus.CREATED,
                command.subscriberId(),
                command.agentCode(),
                null,   // version assigned by JPA on first save
                now,
                now
        );
        Quote saved = quoteRepository.save(newQuote);

        // Step 6: return result flagged as newly created
        return new FolioCreationResult(saved, true);
    }
}
