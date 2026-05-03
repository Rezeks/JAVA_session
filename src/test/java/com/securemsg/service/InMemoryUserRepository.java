package com.securemsg.service;

import com.securemsg.domain.User;
import com.securemsg.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.lang.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.Objects;

/**
 * In-memory stub for unit tests (no Spring context needed).
 */
@SuppressWarnings("unchecked")
public class InMemoryUserRepository implements UserRepository {
    private final Map<UUID, User> store = new ConcurrentHashMap<>();

    @Override public Optional<User> findByLogin(String login) {
        return store.values().stream().filter(u -> u.login().equals(login)).findFirst();
    }
    @Override public boolean existsByLogin(String login) {
        return store.values().stream().anyMatch(u -> u.login().equals(login));
    }
    @Override @NonNull public <S extends User> S save(@NonNull S entity) { store.put(entity.id(), entity); return entity; }
    @Override @NonNull public Optional<User> findById(@NonNull UUID id) { return Objects.requireNonNull(Optional.ofNullable(store.get(id))); }
    @Override public boolean existsById(@NonNull UUID id) { return store.containsKey(id); }
    @Override @NonNull public List<User> findAll() { return Objects.requireNonNull(new ArrayList<>(store.values())); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(@NonNull UUID id) { store.remove(id); }
    @Override public void delete(@NonNull User entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    // --- Unused stubs ---
    @Override @NonNull public <S extends User> List<S> saveAll(@NonNull Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override @NonNull public List<User> findAllById(@NonNull Iterable<UUID> ids) { List<User> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return Objects.requireNonNull(r); }
    @Override public void deleteAllById(@NonNull Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(@NonNull Iterable<? extends User> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override @NonNull public <S extends User> S saveAndFlush(@NonNull S entity) { return save(entity); }
    @Override @NonNull public <S extends User> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(@NonNull Iterable<User> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(@NonNull Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override @NonNull public User getOne(@NonNull UUID id) { return Objects.requireNonNull(store.get(id)); }
    @Override @NonNull public User getById(@NonNull UUID id) { return Objects.requireNonNull(store.get(id)); }
    @Override @NonNull public User getReferenceById(@NonNull UUID id) { return Objects.requireNonNull(store.get(id)); }
    @Override @NonNull public <S extends User> Optional<S> findOne(@NonNull Example<S> example) { return Objects.requireNonNull(Optional.empty()); }
    @Override @NonNull public <S extends User> List<S> findAll(@NonNull Example<S> example) { return Objects.requireNonNull(List.of()); }
    @Override @NonNull public <S extends User> List<S> findAll(@NonNull Example<S> example, @NonNull Sort sort) { return Objects.requireNonNull(List.of()); }
    @Override @NonNull public <S extends User> Page<S> findAll(@NonNull Example<S> example, @NonNull Pageable pageable) { return Objects.requireNonNull(Page.empty()); }
    @Override public <S extends User> long count(@NonNull Example<S> example) { return 0; }
    @Override public <S extends User> boolean exists(@NonNull Example<S> example) { return false; }
    @Override @NonNull public <S extends User, R> R findBy(@NonNull Example<S> example, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
    @Override @NonNull public List<User> findAll(@NonNull Sort sort) { return findAll(); }
    @Override @NonNull public Page<User> findAll(@NonNull Pageable pageable) { return new PageImpl<>(findAll()); }
}
