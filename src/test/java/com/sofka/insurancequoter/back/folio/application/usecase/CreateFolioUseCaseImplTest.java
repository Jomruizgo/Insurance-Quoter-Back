package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateFolioUseCaseImplTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private CoreServiceClient coreServiceClient;

    @InjectMocks
    private CreateFolioUseCaseImpl useCase;

    // --- CRITERIO-1.2: Idempotencia — folio existente en CREATED ---

    @Test
    void shouldReturnExistingQuote_whenActiveCreatedFolioAlreadyExists() {
        // GIVEN
        var existingQuote = buildQuote("FOL-2026-00042", QuoteStatus.CREATED);
        var command = new CreateFolioCommand("SUB-001", "AGT-123");
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-001", "AGT-123"))
                .thenReturn(Optional.of(existingQuote));

        // WHEN
        FolioCreationResult result = useCase.createFolio(command);

        // THEN
        assertThat(result.created()).isFalse();
        assertThat(result.quote().folioNumber()).isEqualTo("FOL-2026-00042");
        // Core service must NOT be called when idempotency applies
        verifyNoInteractions(coreServiceClient);
        verify(quoteRepository, never()).save(any());
    }

    // --- CRITERIO-1.1: Creación exitosa de folio nuevo ---

    @Test
    void shouldCreateNewFolio_whenNoActiveFolioExistsAndReferencesAreValid() {
        // GIVEN
        var command = new CreateFolioCommand("SUB-001", "AGT-123");
        var savedQuote = buildQuote("FOL-2026-00042", QuoteStatus.CREATED);
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-001", "AGT-123"))
                .thenReturn(Optional.empty());
        when(coreServiceClient.existsSubscriber("SUB-001")).thenReturn(true);
        when(coreServiceClient.existsAgent("AGT-123")).thenReturn(true);
        when(coreServiceClient.nextFolioNumber()).thenReturn("FOL-2026-00042");
        when(quoteRepository.save(any())).thenReturn(savedQuote);

        // WHEN
        FolioCreationResult result = useCase.createFolio(command);

        // THEN
        assertThat(result.created()).isTrue();
        assertThat(result.quote().folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(result.quote().quoteStatus()).isEqualTo(QuoteStatus.CREATED);
        verify(quoteRepository).save(any(Quote.class));
    }

    // --- CRITERIO-1.3: subscriberId inválido ---

    @Test
    void shouldThrowInvalidReferenceException_whenSubscriberDoesNotExist() {
        // GIVEN
        var command = new CreateFolioCommand("SUB-999", "AGT-123");
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-999", "AGT-123"))
                .thenReturn(Optional.empty());
        when(coreServiceClient.existsSubscriber("SUB-999")).thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.createFolio(command))
                .isInstanceOf(InvalidReferenceException.class);
        verify(coreServiceClient, never()).existsAgent(any());
        verify(quoteRepository, never()).save(any());
    }

    // --- CRITERIO-1.3: agentCode inválido ---

    @Test
    void shouldThrowInvalidReferenceException_whenAgentDoesNotExist() {
        // GIVEN
        var command = new CreateFolioCommand("SUB-001", "AGT-999");
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-001", "AGT-999"))
                .thenReturn(Optional.empty());
        when(coreServiceClient.existsSubscriber("SUB-001")).thenReturn(true);
        when(coreServiceClient.existsAgent("AGT-999")).thenReturn(false);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.createFolio(command))
                .isInstanceOf(InvalidReferenceException.class);
        verify(quoteRepository, never()).save(any());
    }

    // --- CRITERIO-1.5: folio existente en IN_PROGRESS no bloquea nuevo folio ---

    @Test
    void shouldCreateNewFolio_whenExistingFolioIsInProgress() {
        // GIVEN — repository returns empty (only CREATED status is considered active for idempotency)
        var command = new CreateFolioCommand("SUB-001", "AGT-123");
        var savedQuote = buildQuote("FOL-2026-00099", QuoteStatus.CREATED);
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-001", "AGT-123"))
                .thenReturn(Optional.empty());
        when(coreServiceClient.existsSubscriber("SUB-001")).thenReturn(true);
        when(coreServiceClient.existsAgent("AGT-123")).thenReturn(true);
        when(coreServiceClient.nextFolioNumber()).thenReturn("FOL-2026-00099");
        when(quoteRepository.save(any())).thenReturn(savedQuote);

        // WHEN
        FolioCreationResult result = useCase.createFolio(command);

        // THEN
        assertThat(result.created()).isTrue();
        assertThat(result.quote().quoteStatus()).isEqualTo(QuoteStatus.CREATED);
    }

    // --- Helper ---

    private Quote buildQuote(String folioNumber, QuoteStatus status) {
        return new Quote(
                folioNumber,
                status,
                "SUB-001",
                "AGT-123",
                1L,
                Instant.parse("2026-04-20T14:30:00Z"),
                Instant.parse("2026-04-20T14:30:00Z")
        );
    }
}
