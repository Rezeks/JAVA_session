package com.securemsg.service;

import com.securemsg.domain.GroupChat;
import com.securemsg.repository.GroupChatRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory stub for unit tests.
 */
@SuppressWarnings("unchecked")
public class InMemoryGroupChatRepository implements GroupChatRepository {
    private final Map<UUID, GroupChat> store = new ConcurrentHashMap<>();

    @Override @NonNull public <S extends GroupChat> S save(@NonNull S entity) { store.put(entity.id(), entity); return entity; }
    @Override @NonNull public Optional<GroupChat> findById(@NonNull UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(@NonNull UUID id) { return store.containsKey(id); }
    @Override @NonNull public List<GroupChat> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(@NonNull UUID id) { store.remove(id); }
    @Override public void delete(@NonNull GroupChat entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override @NonNull public <S extends GroupChat> List<S> saveAll(@NonNull Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override @NonNull public List<GroupChat> findAllById(@NonNull Iterable<UUID> ids) { List<GroupChat> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(@NonNull Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(@NonNull Iterable<? extends GroupChat> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override @NonNull public <S extends GroupChat> S saveAndFlush(@NonNull S entity) { return save(entity); }
    @Override @NonNull public <S extends GroupChat> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(@NonNull Iterable<GroupChat> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(@NonNull Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override @NonNull public GroupChat getOne(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public GroupChat getById(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public GroupChat getReferenceById(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public <S extends GroupChat> Optional<S> findOne(@NonNull Example<S> example) { return Optional.empty(); }
    @Override @NonNull public <S extends GroupChat> List<S> findAll(@NonNull Example<S> example) { return List.of(); }
    @Override @NonNull public <S extends GroupChat> List<S> findAll(@NonNull Example<S> example, @NonNull Sort sort) { return List.of(); }
    @Override @NonNull public <S extends GroupChat> Page<S> findAll(@NonNull Example<S> example, @NonNull Pageable pageable) { return Page.empty(); }
    @Override public <S extends GroupChat> long count(@NonNull Example<S> example) { return 0; }
    @Override public <S extends GroupChat> boolean exists(@NonNull Example<S> example) { return false; }
    @Override @NonNull public <S extends GroupChat, R> R findBy(@NonNull Example<S> example, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return (R) null; }
    @Override @NonNull public List<GroupChat> findAll(@NonNull Sort sort) { return findAll(); }
    @Override @NonNull public Page<GroupChat> findAll(@NonNull Pageable pageable) { return new PageImpl<>(findAll()); }
}
