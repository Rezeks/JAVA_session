package com.securemsg.service;

import com.securemsg.domain.AuditEvent;
import com.securemsg.repository.AuditEventRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.Objects;

/**
 * In-memory stub for unit tests.
 */
@SuppressWarnings("unchecked")
class InMemoryAuditEventRepository implements AuditEventRepository {
    private final Map<UUID, AuditEvent> store = new ConcurrentHashMap<>();

    @Override @NonNull public <S extends AuditEvent> S save(@NonNull S entity) { store.put(entity.id(), entity); return entity; }
    @Override @NonNull public Optional<AuditEvent> findById(@NonNull UUID id) { return Objects.requireNonNull(Optional.ofNullable(store.get(id))); }
    @Override public boolean existsById(@NonNull UUID id) { return store.containsKey(id); }
    @Override @NonNull public List<AuditEvent> findAll() { return Objects.requireNonNull(new ArrayList<>(store.values())); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(@NonNull UUID id) { store.remove(id); }
    @Override public void delete(@NonNull AuditEvent entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override @NonNull public <S extends AuditEvent> List<S> saveAll(@NonNull Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override @NonNull public List<AuditEvent> findAllById(@NonNull Iterable<UUID> ids) { List<AuditEvent> r = new ArrayList<>(); for (UUID id : ids) { if (id != null) findById(id).ifPresent(r::add); } return Objects.requireNonNull(r); }
    @Override public void deleteAllById(@NonNull Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(@NonNull Iterable<? extends AuditEvent> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override @NonNull public <S extends AuditEvent> S saveAndFlush(@NonNull S entity) { return save(entity); }
    @Override @NonNull public <S extends AuditEvent> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(@NonNull Iterable<AuditEvent> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(@NonNull Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override @NonNull public AuditEvent getOne(@NonNull UUID id) { return Objects.requireNonNull(store.get(id)); }
    @Override @NonNull public AuditEvent getById(@NonNull UUID id) { return Objects.requireNonNull(store.get(id)); }
    @Override @NonNull public AuditEvent getReferenceById(@NonNull UUID id) { return Objects.requireNonNull(store.get(id)); }
    @Override @NonNull public <S extends AuditEvent> Optional<S> findOne(@NonNull Example<S> example) { return Objects.requireNonNull(Optional.empty()); }
    @Override @NonNull public <S extends AuditEvent> List<S> findAll(@NonNull Example<S> example) { return Objects.requireNonNull(List.of()); }
    @Override @NonNull public <S extends AuditEvent> List<S> findAll(@NonNull Example<S> example, @NonNull Sort sort) { return Objects.requireNonNull(List.of()); }
    @Override @NonNull public <S extends AuditEvent> Page<S> findAll(@NonNull Example<S> example, @NonNull Pageable pageable) { return Objects.requireNonNull(Page.empty()); }
    @Override public <S extends AuditEvent> long count(@NonNull Example<S> example) { return 0; }
    @Override public <S extends AuditEvent> boolean exists(@NonNull Example<S> example) { return false; }
    @Override @NonNull public <S extends AuditEvent, R> R findBy(@NonNull Example<S> example, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    @Override @NonNull public List<AuditEvent> findAll(@NonNull Sort sort) { return findAll(); }
    @Override @NonNull public Page<AuditEvent> findAll(@NonNull Pageable pageable) { return new PageImpl<>(findAll()); }
}
