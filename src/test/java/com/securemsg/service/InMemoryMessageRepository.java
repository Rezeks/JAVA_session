package com.securemsg.service;

import com.securemsg.domain.DeliveryStatus;
import com.securemsg.domain.Message;
import com.securemsg.repository.MessageRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * In-memory stub for unit tests.
 */
@SuppressWarnings("unchecked")
public class InMemoryMessageRepository implements MessageRepository {
    private final Map<UUID, Message> store = new ConcurrentHashMap<>();

    @Override public List<Message> findByRecipientIdAndStatus(UUID recipientId, DeliveryStatus status) {
        return store.values().stream()
                .filter(m -> m.recipientId().equals(recipientId) && m.status() == status)
                .collect(Collectors.toList());
    }

    @Override public List<Message> findBySenderIdOrRecipientId(UUID senderId, UUID recipientId) {
        return store.values().stream()
                .filter(m -> m.senderId().equals(senderId) || m.recipientId().equals(recipientId))
                .collect(Collectors.toList());
    }

    @Override @NonNull public <S extends Message> S save(@NonNull S entity) { store.put(entity.id(), entity); return entity; }
    @Override @NonNull public Optional<Message> findById(@NonNull UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(@NonNull UUID id) { return store.containsKey(id); }
    @Override @NonNull public List<Message> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(@NonNull UUID id) { store.remove(id); }
    @Override public void delete(@NonNull Message entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override @NonNull public <S extends Message> List<S> saveAll(@NonNull Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override @NonNull public List<Message> findAllById(@NonNull Iterable<UUID> ids) { List<Message> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(@NonNull Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(@NonNull Iterable<? extends Message> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override @NonNull public <S extends Message> S saveAndFlush(@NonNull S entity) { return save(entity); }
    @Override @NonNull public <S extends Message> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(@NonNull Iterable<Message> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(@NonNull Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override @NonNull public Message getOne(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public Message getById(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public Message getReferenceById(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public <S extends Message> Optional<S> findOne(@NonNull Example<S> example) { return Optional.empty(); }
    @Override @NonNull public <S extends Message> List<S> findAll(@NonNull Example<S> example) { return List.of(); }
    @Override @NonNull public <S extends Message> List<S> findAll(@NonNull Example<S> example, @NonNull Sort sort) { return List.of(); }
    @Override @NonNull public <S extends Message> Page<S> findAll(@NonNull Example<S> example, @NonNull Pageable pageable) { return Page.empty(); }
    @Override public <S extends Message> long count(@NonNull Example<S> example) { return 0; }
    @Override public <S extends Message> boolean exists(@NonNull Example<S> example) { return false; }
    @Override @NonNull public <S extends Message, R> R findBy(@NonNull Example<S> example, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return (R) null; }
    @Override @NonNull public List<Message> findAll(@NonNull Sort sort) { return findAll(); }
    @Override @NonNull public Page<Message> findAll(@NonNull Pageable pageable) { return new PageImpl<>(findAll()); }
}
