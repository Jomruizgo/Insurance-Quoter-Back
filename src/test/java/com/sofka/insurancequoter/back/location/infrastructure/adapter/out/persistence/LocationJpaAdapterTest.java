package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter.LocationJpaAdapter;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class LocationJpaAdapterTest {

    @Mock
    private LocationJpaRepository locationJpaRepository;

    @Mock
    private LocationPersistenceMapper mapper;

    @InjectMocks
    private LocationJpaAdapter adapter;

    @Test
    void shouldReturnActiveLocations_whenQuoteHasActiveLocations() {
        // GIVEN
        LocationJpa jpa1 = LocationJpa.builder().id(1L).quoteId(10L).index(1).active(true).build();
        LocationJpa jpa2 = LocationJpa.builder().id(2L).quoteId(10L).index(2).active(true).build();
        when(locationJpaRepository.findByQuoteIdAndActiveTrue(10L)).thenReturn(List.of(jpa1, jpa2));
        when(mapper.toDomain(jpa1)).thenReturn(new Location(1, true));
        when(mapper.toDomain(jpa2)).thenReturn(new Location(2, true));

        // WHEN
        List<Location> result = adapter.findActiveByQuoteId(10L);

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Location::index).containsExactly(1, 2);
    }

    @Test
    void shouldReturnAllLocations_includesInactive() {
        // GIVEN
        LocationJpa active = LocationJpa.builder().id(1L).quoteId(10L).index(1).active(true).build();
        LocationJpa inactive = LocationJpa.builder().id(2L).quoteId(10L).index(2).active(false).build();
        when(locationJpaRepository.findByQuoteId(10L)).thenReturn(List.of(active, inactive));
        when(mapper.toDomain(active)).thenReturn(new Location(1, true));
        when(mapper.toDomain(inactive)).thenReturn(new Location(2, false));

        // WHEN
        List<Location> result = adapter.findAllByQuoteId(10L);

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Location::active).containsExactly(true, false);
    }

    @Test
    void shouldInsertNewLocations_whenInsertAllCalled() {
        // GIVEN
        List<Location> newLocations = List.of(new Location(3, true), new Location(4, true));
        LocationJpa jpa3 = LocationJpa.builder().quoteId(10L).index(3).active(true).build();
        LocationJpa jpa4 = LocationJpa.builder().quoteId(10L).index(4).active(true).build();
        when(mapper.toLocationJpa(10L, new Location(3, true))).thenReturn(jpa3);
        when(mapper.toLocationJpa(10L, new Location(4, true))).thenReturn(jpa4);

        // WHEN
        adapter.insertAll(10L, newLocations);

        // THEN
        ArgumentCaptor<List<LocationJpa>> captor = ArgumentCaptor.forClass(List.class);
        verify(locationJpaRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void shouldDoNothing_whenInsertAllWithEmptyList() {
        // WHEN
        adapter.insertAll(10L, List.of());

        // THEN
        verifyNoInteractions(locationJpaRepository);
    }

    @Test
    void shouldSetActiveFalse_whenDeactivateByIndicesCalled() {
        // GIVEN
        LocationJpa jpa3 = LocationJpa.builder().id(3L).quoteId(10L).index(3).active(true).build();
        LocationJpa jpa4 = LocationJpa.builder().id(4L).quoteId(10L).index(4).active(true).build();
        when(locationJpaRepository.findByQuoteIdAndIndexIn(10L, List.of(3, 4)))
                .thenReturn(List.of(jpa3, jpa4));

        // WHEN
        adapter.deactivateByIndices(10L, List.of(3, 4));

        // THEN
        assertThat(jpa3.getActive()).isFalse();
        assertThat(jpa4.getActive()).isFalse();
        verify(locationJpaRepository).saveAll(List.of(jpa3, jpa4));
    }

    @Test
    void shouldSetActiveTrue_whenReactivateByIndicesCalled() {
        // GIVEN
        LocationJpa jpa3 = LocationJpa.builder().id(3L).quoteId(10L).index(3).active(false).build();
        LocationJpa jpa4 = LocationJpa.builder().id(4L).quoteId(10L).index(4).active(false).build();
        when(locationJpaRepository.findByQuoteIdAndIndexIn(10L, List.of(3, 4)))
                .thenReturn(List.of(jpa3, jpa4));

        // WHEN
        adapter.reactivateByIndices(10L, List.of(3, 4));

        // THEN
        assertThat(jpa3.getActive()).isTrue();
        assertThat(jpa4.getActive()).isTrue();
        verify(locationJpaRepository).saveAll(List.of(jpa3, jpa4));
    }
}
