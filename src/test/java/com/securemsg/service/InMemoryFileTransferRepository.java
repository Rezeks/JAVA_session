package com.securemsg.service;

import com.securemsg.domain.FileTransfer;
import com.securemsg.repository.FileTransferRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * In-memory stub for unit tests.
 */
@SuppressWarnings("unchecked")
class InMemoryFileTransferRepository implements FileTransferRepository {
    private final Map<UUID, FileTransfer> store = new ConcurrentHashMap<>();

    @Override public <S extends FileTransfer> S save(S entity) { store.put(entity.id(), entity); return entity; }
    @Override public Optional<FileTransfer> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    @Override public boolean existsById(UUID id) { return store.containsKey(id); }
    @Override public List<FileTransfer> findAll() { return new ArrayList<>(store.values()); }
    @Override public long count() { return store.size(); }
    @Override public void deleteById(UUID id) { store.remove(id); }
    @Override public void delete(FileTransfer entity) { store.remove(entity.id()); }
    @Override public void deleteAll() { store.clear(); }

    @Override public <S extends FileTransfer> List<S> saveAll(Iterable<S> entities) { entities.forEach(this::save); return (List<S>) findAll(); }
    @Override public List<FileTransfer> findAllById(Iterable<UUID> ids) { List<FileTransfer> r = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(r::add)); return r; }
    @Override public void deleteAllById(Iterable<? extends UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAll(Iterable<? extends FileTransfer> entities) { entities.forEach(this::delete); }
    @Override public void flush() {}
    @Override public <S extends FileTransfer> S saveAndFlush(S entity) { return save(entity); }
    @Override public <S extends FileTransfer> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
    @Override public void deleteAllInBatch(Iterable<FileTransfer> entities) { entities.forEach(this::delete); }
    @Override public void deleteAllByIdInBatch(Iterable<UUID> ids) { ids.forEach(this::deleteById); }
    @Override public void deleteAllInBatch() { store.clear(); }
    @Override public FileTransfer getOne(UUID id) { return store.get(id); }
    @Override public FileTransfer getById(UUID id) { return store.get(id); }
    @Override public FileTransfer getReferenceById(UUID id) { return store.get(id); }
    @Override public <S extends FileTransfer> Optional<S> findOne(Example<S> example) { return Optional.empty(); }
    @Override public <S extends FileTransfer> List<S> findAll(Example<S> example) { return List.of(); }
    @Override public <S extends FileTransfer> List<S> findAll(Example<S> example, Sort sort) { return List.of(); }
    @Override public <S extends FileTransfer> Page<S> findAll(Example<S> example, Pageable pageable) { return Page.empty(); }
    @Override public <S extends FileTransfer> long count(Example<S> example) { return 0; }
    @Override public <S extends FileTransfer> boolean exists(Example<S> example) { return false; }
    @Override public <S extends FileTransfer, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    @Override public List<FileTransfer> findAll(Sort sort) { return findAll(); }
    @Override public Page<FileTransfer> findAll(Pageable pageable) { return new PageImpl<>(findAll()); }
}
