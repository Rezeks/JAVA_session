package com.securemsg.service;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.Message;
import com.securemsg.repository.MessageRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * In-memory stub for unit tests.
 */
@SuppressWarnings("unchecked")
class InMemoryMessageRepository implements MessageRepository {
    private final Map<UUID, Message> store = new ConcurrentHashMap<>();

    @Override public List<Message> findBySenderIdOrRecipientId(UUID senderId, UUID recipientId) {
        return store.values().stream()
                .filter(m -> m.senderId().equals(senderId) || m.recipientId().equals(recipientId))
                .collect(Collectors.toList());
    }
    @Override public List<Message> findByRecipientIdAndStatus(UUID recipientId, DeliveryStatus status) {
        return store.values().stream()
                .filter(m -> m.recipientId().equals(recipientId) && m.status() == status)
                .collect(Collectors.toList());
    }
    @Override public <S extends Message> S save(S entity) { store.put(entity.id(), entity); return entity; }
    @Override public Optional<Message> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(UUID id) { return store.containsKey(id); }
    @Override public List<Message> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(UUID id) { store.remove(id); }
    @Override public void delete(Message entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override public <S extends Message> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override public List<Message> findAllById(Iterable<UUID> ids) { List<Message> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends Message> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override public <S extends Message> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends Message> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<Message> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override public Message getOne(UUID id) { return store.get(id); }
    @Override public Message getById(UUID id) { return store.get(id); }
    @Override public Message getReferenceById(UUID id) { return store.get(id); }
    @Override public <S extends Message> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends Message> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends Message> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends Message> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends Message> long count(Example<S> example) { return 0; }
    @Override public <S extends Message> boolean exists(Example<S> example) { return false; }
    @Override public <S extends Message, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<Message> findAll(Sort sort) { return findAll(); }
    @Override public Page<Message> findAll(Pageable pageable) { return new PageImpl<>(findAll()); }
}
