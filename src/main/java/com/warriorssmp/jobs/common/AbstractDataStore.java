package com.warriorssmp.jobs.common;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generic per-player data cache shared by every skill. This is the actual
 * fix for the ConcurrentModificationException bug that hit WSMP-Cooking —
 * get() used to be a single `cache.computeIfAbsent(uuid, this::load)` call,
 * which HashMap forbids touching again while it's running, including
 * reentrantly from the same thread (e.g. a scoreboard/placeholder plugin
 * querying this same player's data mid-join, before the call returns).
 * That was copy-pasted identically into all six skills' DataStore classes,
 * so it needed fixing in six places before; now it's fixed once, here, and
 * every skill's own DataStore just extends this and implements load()/save()
 * for its own PlayerGatherData subtype.
 */
public abstract class AbstractDataStore<T> {

    protected final JobModule module;
    protected final File folder;
    private final Map<UUID, T> cache = new HashMap<>();

    protected AbstractDataStore(JobModule module, String subfolder) {
        this.module = module;
        this.folder = new File(module.getDataFolder(), subfolder);
        if (!folder.exists()) folder.mkdirs();
    }

    /** Reads (or creates fresh, if no file exists yet) one player's data. */
    protected abstract T load(UUID uuid);

    /** Writes one player's data to disk. */
    public abstract void save(T data);

    public T get(UUID uuid) {
        // Plain get-then-put instead of computeIfAbsent — see class javadoc.
        T existing = cache.get(uuid);
        if (existing != null) return existing;
        T loaded = load(uuid);
        cache.put(uuid, loaded);
        return loaded;
    }

    public void unload(UUID uuid) {
        T data = cache.remove(uuid);
        if (data != null) save(data);
    }

    public void saveAll() {
        for (T data : cache.values()) {
            save(data);
        }
    }

    /** For leaderboard scans / admin lookups — merges currently-loaded
     *  (online) player data with everyone saved to disk. */
    public List<T> allKnownPlayers() {
        Map<UUID, T> merged = new HashMap<>(cache);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                try {
                    UUID uuid = UUID.fromString(f.getName().replace(".yml", ""));
                    if (!merged.containsKey(uuid)) merged.put(uuid, load(uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return new ArrayList<>(merged.values());
    }
}
