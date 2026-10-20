package dev.ricodevvv.riven.data.cosmetic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cosmetics a player owns and the one selected per type. Lives inside the profile.
 */
public class CosmeticData {

    private Map<String, Set<String>> owned = new LinkedHashMap<>();
    private Map<String, String> selected = new LinkedHashMap<>();

    public boolean owns(String type, String id) {
        final Set<String> ids = this.owned.get(type.toLowerCase());

        return ids != null && ids.contains(id.toLowerCase());
    }

    public void give(String type, String id) {
        this.owned.computeIfAbsent(type.toLowerCase(), key -> new LinkedHashSet<>()).add(id.toLowerCase());
    }

    public void take(String type, String id) {
        final Set<String> ids = this.owned.get(type.toLowerCase());

        if (ids != null) {
            ids.remove(id.toLowerCase());
        }

        if (id.equalsIgnoreCase(this.selected.get(type.toLowerCase()))) {
            this.selected.remove(type.toLowerCase());
        }
    }

    public String getSelected(String type) {
        return this.selected.get(type.toLowerCase());
    }

    public void select(String type, String id) {
        if (id == null) {
            this.selected.remove(type.toLowerCase());
        } else {
            this.selected.put(type.toLowerCase(), id.toLowerCase());
        }
    }

    public Set<String> getOwned(String type) {
        final Set<String> ids = this.owned.get(type.toLowerCase());

        return ids == null ? Collections.emptySet() : Collections.unmodifiableSet(ids);
    }

}
