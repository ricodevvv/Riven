package dev.ricodevvv.riven.game.vote;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The votes of one game. Each player gets one vote per category; ranks can weigh more with the
 * {@code <prefix>.power.<n>} permission (so VIPs count double, for example). Ties go to the default option
 * if it's among the tied ones, otherwise to a random one of them.
 */
public class VoteSession {

    private final Map<String, VoteCategory<?>> categories = new LinkedHashMap<>();
    private final Map<String, Map<UUID, String>> votes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> power = new ConcurrentHashMap<>();
    private final Map<String, VoteOption<?>> forced = new ConcurrentHashMap<>();
    private final String permissionPrefix;
    private final Random random = new Random();
    private boolean closed;

    /**
     * @param permissionPrefix prefix for option permissions and vote power, like "skywars.votes"
     */
    public VoteSession(String permissionPrefix) {
        this.permissionPrefix = permissionPrefix;
    }

    public VoteSession category(VoteCategory<?> category) {
        this.categories.put(category.getId(), category);
        this.votes.put(category.getId(), new ConcurrentHashMap<>());
        return this;
    }

    /**
     * Votes for an option, replacing the previous vote of the player in that category.
     *
     * @param player voter
     * @param categoryId category
     * @param optionId option
     * @return how it went
     */
    public Result vote(Player player, String categoryId, String optionId) {
        if (this.closed) {
            return Result.CLOSED;
        }

        final VoteCategory<?> category = this.categories.get(categoryId.toLowerCase());
        final VoteOption<?> option = category == null ? null : category.getOption(optionId);

        if (option == null) {
            return Result.UNKNOWN;
        }

        if (!this.canVote(player, category, option)) {
            return Result.NO_PERMISSION;
        }

        final String previous = this.votes.get(category.getId()).put(player.getUniqueId(), option.getId());

        this.power.put(player.getUniqueId(), this.powerOf(player));
        return option.getId().equals(previous) ? Result.SAME : Result.SUCCESS;
    }

    /**
     * @param player voter
     * @param category category
     * @param option option
     * @return true if they have the option permission, the category wildcard or the option has none
     */
    public boolean canVote(Player player, VoteCategory<?> category, VoteOption<?> option) {
        if (option.getPermission() == null) {
            return true;
        }
        return player.hasPermission(option.getPermission()) || player.hasPermission(this.permissionPrefix + "." + category.getId() + ".*");
    }

    private int powerOf(Player player) {
        int best = 1;
        final String prefix = this.permissionPrefix + ".power.";

        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getValue() && info.getPermission().startsWith(prefix)) {
                try {
                    best = Math.max(best, Integer.parseInt(info.getPermission().substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return best;
    }

    /**
     * Drops every vote of a player (when they leave the game).
     *
     * @param playerId player
     */
    public void remove(UUID playerId) {
        for (Map<UUID, String> categoryVotes : this.votes.values()) {
            categoryVotes.remove(playerId);
        }

        this.power.remove(playerId);
    }

    /**
     * @param categoryId category
     * @param optionId option
     * @return weighted votes of an option
     */
    public int getVotes(String categoryId, String optionId) {
        final Map<UUID, String> categoryVotes = this.votes.get(categoryId.toLowerCase());

        if (categoryVotes == null) {
            return 0;
        }

        int total = 0;

        for (Map.Entry<UUID, String> entry : categoryVotes.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(optionId)) {
                total += this.power.getOrDefault(entry.getKey(), 1);
            }
        }
        return total;
    }

    /**
     * @param playerId voter
     * @param categoryId category
     * @return the option id they voted, or null
     */
    public String getVote(UUID playerId, String categoryId) {
        final Map<UUID, String> categoryVotes = this.votes.get(categoryId.toLowerCase());

        return categoryVotes == null ? null : categoryVotes.get(playerId);
    }

    /**
     * Forces the result of a category, ignoring votes (admins, hosted games).
     *
     * @param categoryId category
     * @param optionId option, null to go back to votes
     */
    public void force(String categoryId, String optionId) {
        final VoteCategory<?> category = this.categories.get(categoryId.toLowerCase());

        if (category == null) {
            return;
        }

        if (optionId == null) {
            this.forced.remove(category.getId());
        } else if (category.getOption(optionId) != null) {
            this.forced.put(category.getId(), category.getOption(optionId));
        }
    }

    /**
     * @param categoryId category
     * @param <T> value type
     * @return the winning option right now
     */
    @SuppressWarnings("unchecked")
    public <T> VoteOption<T> getWinner(String categoryId) {
        final VoteCategory<T> category = (VoteCategory<T>) this.categories.get(categoryId.toLowerCase());

        if (category == null) {
            return null;
        }

        final VoteOption<?> forcedOption = this.forced.get(category.getId());

        if (forcedOption != null) {
            return (VoteOption<T>) forcedOption;
        }

        int best = 0;
        final List<VoteOption<T>> tied = new ArrayList<>();

        for (VoteOption<T> option : category.getOptions()) {
            final int votes = this.getVotes(category.getId(), option.getId());

            if (votes > best) {
                best = votes;
                tied.clear();
                tied.add(option);
            } else if (votes == best && votes > 0) {
                tied.add(option);
            }
        }

        if (tied.isEmpty()) {
            return category.getDefault();
        }

        if (tied.contains(category.getDefault())) {
            return category.getDefault();
        }
        return tied.get(this.random.nextInt(tied.size()));
    }

    /**
     * @param categoryId category
     * @param fallback value if the category doesn't exist
     * @param <T> value type
     * @return the value of the winning option
     */
    public <T> T getWinnerValue(String categoryId, T fallback) {
        final VoteOption<T> winner = this.getWinner(categoryId);

        return winner == null ? fallback : winner.getValue();
    }

    /**
     * Stops accepting votes (when the game starts).
     */
    public void close() {
        this.closed = true;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public VoteCategory<?> getCategory(String id) {
        return this.categories.get(id.toLowerCase());
    }

    public Collection<VoteCategory<?>> getCategories() {
        return Collections.unmodifiableCollection(this.categories.values());
    }

    public String getPermissionPrefix() {
        return this.permissionPrefix;
    }

    /**
     * How a vote went.
     */
    public enum Result {
        SUCCESS,
        SAME,
        NO_PERMISSION,
        UNKNOWN,
        CLOSED
    }

}
