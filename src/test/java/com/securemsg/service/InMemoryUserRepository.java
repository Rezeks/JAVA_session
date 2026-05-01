package com.securemsg.service;

import com.securemsg.domain.User;
import com.securemsg.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory stub for unit tests (no Spring context needed).
 */
@SuppressWarnings("unchecked")
class InMemoryUserRepository implements UserRepository {
    private final Map<UUID, User> store = new ConcurrentHashMap<>();

    @Override public Optional<User> findByLogin(String login) {
        return store.values().stream().filter(u -> u.login().equals(login)).findFirst();
    }
    @Override public boolean existsByLogin(String login) {
        return store.values().stream().anyMatch(u -> u.login().equals(login));
    }
    @Override public <S extends User> S save(S entity) { store.put(entity.id(), entity); return entity; }
    @Override public Optional<User> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(UUID id) { return store.containsKey(id); }
    @Override public List<User> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(UUID id) { store.remove(id); }
    @Override public void delete(User entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    // --- Unused stubs ---
    @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override public List<User> findAllById(Iterable<UUID> ids) { List<User> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends User> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<User> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override public User getOne(UUID id) { return store.get(id); }
    @Override public User getById(UUID id) { return store.get(id); }
    @Override public User getReferenceById(UUID id) { return store.get(id); }
    @Override public <S extends User> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends User> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends User> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends User> long count(Example<S> example) { return 0; }
    @Override public <S extends User> boolean exists(Example<S> example) { return false; }
    @Override public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<User> findAll(Sort sort) { return findAll(); }
    @Override public Page<User> findAll(Pageable pageable) { return new PageImpl<>(findAll()); }
}
