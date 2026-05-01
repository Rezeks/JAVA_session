package com.securemsg.service;

import com.securemsg.domain.AuditEvent;
import com.securemsg.repository.AuditEventRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory stub for unit tests.
 */
@SuppressWarnings("unchecked")
class InMemoryAuditEventRepository implements AuditEventRepository {
    private final Map<UUID, AuditEvent> store = new ConcurrentHashMap<>();

    @Override public <S extends AuditEvent> S save(S entity) { store.put(entity.id(), entity); return entity; }
    @Override public Optional<AuditEvent> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(UUID id) { return store.containsKey(id); }
    @Override public List<AuditEvent> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(UUID id) { store.remove(id); }
    @Override public void delete(AuditEvent entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override public <S extends AuditEvent> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override public List<AuditEvent> findAllById(Iterable<UUID> ids) { List<AuditEvent> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends AuditEvent> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override public <S extends AuditEvent> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends AuditEvent> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<AuditEvent> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override public AuditEvent getOne(UUID id) { return store.get(id); }
    @Override public AuditEvent getById(UUID id) { return store.get(id); }
    @Override public AuditEvent getReferenceById(UUID id) { return store.get(id); }
    @Override public <S extends AuditEvent> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends AuditEvent> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends AuditEvent> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends AuditEvent> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends AuditEvent> long count(Example<S> example) { return 0; }
    @Override public <S extends AuditEvent> boolean exists(Example<S> example) { return false; }
    @Override public <S extends AuditEvent, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<AuditEvent> findAll(Sort sort) { return findAll(); }
    @Override public Page<AuditEvent> findAll(Pageable pageable) { return new PageImpl<>(findAll()); }
}
