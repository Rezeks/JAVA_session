package com.securemsg.service;

import com.securemsg.domain.FileTransfer;
import com.securemsg.repository.FileTransferRepository;
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
class InMemoryFileTransferRepository implements FileTransferRepository {
    private final Map<UUID, FileTransfer> store = new ConcurrentHashMap<>();

    @Override @NonNull public <S extends FileTransfer> S save(@NonNull S entity) { store.put(entity.id(), entity); return entity; }
    @Override @NonNull public Optional<FileTransfer> findById(@NonNull UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(@NonNull UUID id) { return store.containsKey(id); }
    @Override @NonNull public List<FileTransfer> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(@NonNull UUID id) { store.remove(id); }
    @Override public void delete(@NonNull FileTransfer entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override @NonNull public <S extends FileTransfer> List<S> saveAll(@NonNull Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override @NonNull public List<FileTransfer> findAllById(@NonNull Iterable<UUID> ids) { List<FileTransfer> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(@NonNull Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(@NonNull Iterable<? extends FileTransfer> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override @NonNull public <S extends FileTransfer> S saveAndFlush(@NonNull S entity) { return save(entity); }
    @Override @NonNull public <S extends FileTransfer> List<S> saveAllAndFlush(@NonNull Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(@NonNull Iterable<FileTransfer> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(@NonNull Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override @NonNull public FileTransfer getOne(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public FileTransfer getById(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public FileTransfer getReferenceById(@NonNull UUID id) { return store.get(id); }
    @Override @NonNull public <S extends FileTransfer> Optional<S> findOne(@NonNull Example<S> example) { return Optional.empty(); }
    @Override @NonNull public <S extends FileTransfer> List<S> findAll(@NonNull Example<S> example) { return List.of(); }
    @Override @NonNull public <S extends FileTransfer> List<S> findAll(@NonNull Example<S> example, @NonNull Sort sort) { return List.of(); }
    @Override @NonNull public <S extends FileTransfer> Page<S> findAll(@NonNull Example<S> example, @NonNull Pageable pageable) { return Page.empty(); }
    @Override public <S extends FileTransfer> long count(@NonNull Example<S> example) { return 0; }
    @Override public <S extends FileTransfer> boolean exists(@NonNull Example<S> example) { return false; }
    @Override @NonNull public <S extends FileTransfer, R> R findBy(@NonNull Example<S> example, @NonNull Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return (R) null; }
    @Override @NonNull public List<FileTransfer> findAll(@NonNull Sort sort) { return findAll(); }
    @Override @NonNull public Page<FileTransfer> findAll(@NonNull Pageable pageable) { return new PageImpl<>(findAll()); }
}
