package dev.ricodevvv.riven.party;

import dev.ricodevvv.riven.Riven;
import dev.ricodevvv.riven.language.LanguageManager;
import dev.ricodevvv.riven.party.event.PartyChatEvent;
import dev.ricodevvv.riven.party.event.PartyDisbandEvent;
import dev.ricodevvv.riven.party.event.PartyInviteEvent;
import dev.ricodevvv.riven.party.event.PartyJoinEvent;
import dev.ricodevvv.riven.party.event.PartyLeaderChangeEvent;
import dev.ricodevvv.riven.party.event.PartyLeaveEvent;
import dev.ricodevvv.riven.player.PlayerLookup;
import dev.ricodevvv.riven.text.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Parties: invites that expire, leader, open/closed parties, kicks, silent joins for staff and party chat.
 * Members that disconnect stay for a while ({@link #setOfflineTimeout(int)}) in case they come back.
 *
 * <p>Every message uses the {@code party.*} keys of the plugin's language files and falls back to English
 * when a key is missing, so it works out of the box. Storage, message delivery and player lookups are
 * pluggable, which is how the network module makes parties work across servers.</p>
 */
public class PartyManager implements Listener {

    private static final Map<String, String> DEFAULTS = new HashMap<>();

    static {
        DEFAULTS.put("party.not-in-party", "&cYou're not in a party.");
        DEFAULTS.put("party.not-leader", "&cOnly the party leader can do that.");
        DEFAULTS.put("party.already-in-own", "&cYou're already in a party. Leave it first with &f/party leave&c.");
        DEFAULTS.put("party.already-member", "&c%player% is already in your party.");
        DEFAULTS.put("party.player-not-found", "&cNobody online with that name.");
        DEFAULTS.put("party.not-member", "&c%player% isn't in your party.");
        DEFAULTS.put("party.full", "&cThe party is full (max %max%).");
        DEFAULTS.put("party.joined", "&9Party &8> &b%player% &ejoined the party.");
        DEFAULTS.put("party.left", "&9Party &8> &b%player% &eleft the party.");
        DEFAULTS.put("party.you-left", "&eYou left the party.");
        DEFAULTS.put("party.kicked", "&9Party &8> &b%player% &ewas kicked from the party.");
        DEFAULTS.put("party.kicked-you", "&cYou were kicked from the party.");
        DEFAULTS.put("party.promoted", "&9Party &8> &b%player% &eis now the party leader.");
        DEFAULTS.put("party.leader-offline", "&9Party &8> &eThe leader disconnected, &b%player% &eis in charge now.");
        DEFAULTS.put("party.reconnected", "&9Party &8> &b%player% &ais back.");
        DEFAULTS.put("party.offline-removed", "&9Party &8> &b%player% &ewas removed for being offline too long.");
        DEFAULTS.put("party.disbanded", "&9Party &8> &cThe party was disbanded.");
        DEFAULTS.put("party.disbanded-alone", "&9Party &8> &cThe party was disbanded because only one person was left.");
        DEFAULTS.put("party.invite.self", "&cYou can't invite yourself.");
        DEFAULTS.put("party.invite.sent", "&eYou invited &b%player% &eto your party. They have &c%time% &eseconds to accept.");
        DEFAULTS.put("party.invite.received", "&b%player% &einvited you to their party. You have &c%time% &eseconds to accept.");
        DEFAULTS.put("party.invite.accept", "&a&l[ACCEPT]");
        DEFAULTS.put("party.invite.accept-hover", "&7Click to join the party");
        DEFAULTS.put("party.invite.deny", "&c&l[DENY]");
        DEFAULTS.put("party.invite.deny-hover", "&7Click to deny the invite");
        DEFAULTS.put("party.invite.already-invited", "&c%player% already has a pending invite.");
        DEFAULTS.put("party.invite.already-in-party", "&c%player% is already in another party.");
        DEFAULTS.put("party.invite.none", "&cYou don't have an invite from %player%.");
        DEFAULTS.put("party.invite.denied", "&eYou denied &b%player%&e's invite.");
        DEFAULTS.put("party.invite.denied-sender", "&b%player% &edenied your invite.");
        DEFAULTS.put("party.invite.expired", "&eThe invite from &b%player% &eexpired.");
        DEFAULTS.put("party.invite.expired-sender", "&eYour invite to &b%player% &eexpired.");
        DEFAULTS.put("party.invite.blocked", "&c%player% isn't taking invites right now.");
        DEFAULTS.put("party.list.header", "&9%leader%'s party &7(%size%/%max%)");
        DEFAULTS.put("party.list.leader", " %status% %level% &f%player% &6(leader)");
        DEFAULTS.put("party.list.member", " %status% %level% &f%player%");
        DEFAULTS.put("party.list.online", "&a●");
        DEFAULTS.put("party.list.offline", "&c●");
        DEFAULTS.put("party.chat.format", "&9Party &8> %level% &f%player%&7: &f%message%");
        DEFAULTS.put("party.chat.usage", "&eUse &b/pc <message> &eto talk to your party.");
        DEFAULTS.put("party.chat.toggled-on", "&aEverything you type now goes to party chat. &7(/party chat to turn it off)");
        DEFAULTS.put("party.chat.toggled-off", "&eYou're back to the normal chat.");
        DEFAULTS.put("party.open.opened", "&9Party &8> &aThe party is open, anyone can join with &f/party join %player%&a.");
        DEFAULTS.put("party.open.closed", "&9Party &8> &eThe party is invite only again.");
        DEFAULTS.put("party.open.not-open", "&c%player%'s party isn't open.");
        DEFAULTS.put("party.silent-join.done", "&7You silently joined &b%player%&7's party.");
    }

    private final JavaPlugin plugin;
    private final Map<UUID, Long> offlineSince = new ConcurrentHashMap<>();
    private final Set<UUID> chatToggled = ConcurrentHashMap.newKeySet();

    private PartyBackend backend = new LocalPartyBackend();
    private PlayerLookup lookup = PlayerLookup.LOCAL;
    private PartyNotifier notifier;
    private Function<UUID, String> levelFormatter = uniqueId -> "";
    private BiFunction<Player, Party, Integer> warpHandler;
    private int maxSize = 8;
    private int inviteExpiration = 60;
    private int offlineTimeout = 300;
    private BukkitTask task;

    public PartyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.notifier = this::sendLocal;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);

        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);

        if (this.task != null) {
            this.task.cancel();
        }
    }

    private void tick() {
        for (Party party : this.backend.getParties()) {
            if (!this.backend.isAuthority(party)) {
                continue;
            }

            final List<UUID> expired = party.clearExpiredInvites();

            if (expired.isEmpty()) {
                continue;
            }

            for (UUID target : expired) {
                this.notifier.send(target, "party.invite.expired", "%player%", party.getName(party.getLeader()));
                this.notifier.send(party.getLeader(), "party.invite.expired-sender", "%player%", party.getName(target));
            }

            if (!this.disbandIfAlone(party)) {
                this.backend.save(party);
            }
        }

        final long timeout = this.offlineTimeout * 1000L;

        for (Map.Entry<UUID, Long> entry : new ArrayList<>(this.offlineSince.entrySet())) {
            if (System.currentTimeMillis() - entry.getValue() < timeout) {
                continue;
            }

            this.offlineSince.remove(entry.getKey());

            final Party party = this.backend.getByMember(entry.getKey());

            if (party != null && !this.lookup.isOnline(entry.getKey())) {
                final String name = party.getName(entry.getKey());

                this.removeMember(party, entry.getKey(), PartyLeaveEvent.Reason.OFFLINE);
                this.broadcast(party, "party.offline-removed", "%player%", name);
            }
        }
    }

    public Party getParty(Player player) {
        return this.backend.getByMember(player.getUniqueId());
    }

    public Party getParty(UUID uniqueId) {
        return this.backend.getByMember(uniqueId);
    }

    /**
     * @param player player picking a game
     * @return how many people join with them (1 without a party or if they aren't the leader)
     */
    public int getJoinSize(Player player) {
        final Party party = this.getParty(player);

        return party != null && party.isLeader(player.getUniqueId()) ? party.getOnlineMembers().size() : 1;
    }

    /**
     * Invites someone. If the sender has no party, one gets created.
     *
     * @param sender who invites
     * @param targetName who gets invited
     * @return the result
     */
    public PartyResult invite(Player sender, String targetName) {
        final UUID target = this.lookup.findOnline(targetName);

        if (target == null) {
            this.send(sender, "party.player-not-found");
            return PartyResult.NOT_MEMBER;
        }

        final String name = this.lookup.getName(target);

        if (target.equals(sender.getUniqueId())) {
            this.send(sender, "party.invite.self");
            return PartyResult.SELF;
        }

        Party party = this.getParty(sender);

        if (party != null && !party.isLeader(sender.getUniqueId())) {
            this.send(sender, "party.not-leader");
            return PartyResult.NOT_LEADER;
        }

        if (party != null && party.isMember(target)) {
            this.send(sender, "party.already-member", "%player%", name);
            return PartyResult.ALREADY_MEMBER;
        }

        if (this.getParty(target) != null) {
            this.send(sender, "party.invite.already-in-party", "%player%", name);
            return PartyResult.TARGET_IN_PARTY;
        }

        if (party != null && party.size() >= this.maxSize) {
            this.send(sender, "party.full", "%max%", this.maxSize);
            return PartyResult.FULL;
        }

        if (party != null && party.isInvited(target)) {
            this.send(sender, "party.invite.already-invited", "%player%", name);
            return PartyResult.ALREADY_INVITED;
        }

        final boolean created = party == null;

        if (created) {
            party = new Party(sender.getUniqueId(), sender.getName());
        }

        final PartyInviteEvent event = new PartyInviteEvent(party, target);

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            this.send(sender, "party.invite.blocked", "%player%", name);
            return PartyResult.CANCELLED;
        }

        party.invite(target, name, System.currentTimeMillis() + this.inviteExpiration * 1000L);

        this.backend.save(party);

        this.send(sender, "party.invite.sent", "%player%", name, "%time%", this.inviteExpiration);
        this.notifier.send(target, "party.invite.received", "%player%", sender.getName(), "%time%", this.inviteExpiration);
        this.notifier.send(target, "party.invite.buttons", "%player%", sender.getName());
        return PartyResult.SUCCESS;
    }

    /**
     * Accepts a leader's invite.
     *
     * @param player who accepts
     * @param leaderName leader name
     * @return the result
     */
    public PartyResult accept(Player player, String leaderName) {
        final Party party = this.findInvitingParty(player.getUniqueId(), leaderName);

        if (party == null) {
            this.send(player, "party.invite.none", "%player%", leaderName);
            return PartyResult.NO_INVITE;
        }

        if (this.getParty(player) != null) {
            this.send(player, "party.already-in-own");
            return PartyResult.ALREADY_IN_PARTY;
        }

        if (party.size() >= this.maxSize) {
            party.removeInvite(player.getUniqueId());
            this.backend.save(party);
            this.send(player, "party.full", "%max%", this.maxSize);
            return PartyResult.FULL;
        }

        this.addMember(party, player, false);
        return PartyResult.SUCCESS;
    }

    /**
     * Joins an open party without an invite.
     *
     * @param player who joins
     * @param leaderName leader name
     * @return the result
     */
    public PartyResult join(Player player, String leaderName) {
        final UUID leader = this.lookup.findOnline(leaderName);
        final Party party = leader == null ? null : this.getParty(leader);

        if (party == null || !party.isLeader(leader)) {
            this.send(player, "party.player-not-found");
            return PartyResult.NOT_IN_PARTY;
        }

        if (!party.isOpen() && !party.isInvited(player.getUniqueId())) {
            this.send(player, "party.open.not-open", "%player%", party.getName(leader));
            return PartyResult.CLOSED;
        }

        if (this.getParty(player) != null) {
            this.send(player, "party.already-in-own");
            return PartyResult.ALREADY_IN_PARTY;
        }

        if (party.size() >= this.maxSize) {
            this.send(player, "party.full", "%max%", this.maxSize);
            return PartyResult.FULL;
        }

        this.addMember(party, player, false);
        return PartyResult.SUCCESS;
    }

    /**
     * Joins someone's party without an invite and without announcing it. Meant for staff.
     *
     * @param player staff member
     * @param targetName any member of the party
     * @return the result
     */
    public PartyResult silentJoin(Player player, String targetName) {
        final UUID target = this.lookup.findOnline(targetName);
        final Party party = target == null ? null : this.getParty(target);

        if (party == null) {
            this.send(player, "party.not-member", "%player%", targetName);
            return PartyResult.NOT_IN_PARTY;
        }

        if (this.getParty(player) != null) {
            this.send(player, "party.already-in-own");
            return PartyResult.ALREADY_IN_PARTY;
        }

        this.addMember(party, player, true);
        this.send(player, "party.silent-join.done", "%player%", party.getName(party.getLeader()));
        return PartyResult.SUCCESS;
    }

    private void addMember(Party party, Player player, boolean silent) {
        party.addMember(player.getUniqueId(), player.getName());

        if (silent) {
            party.setHidden(player.getUniqueId());
        }

        this.backend.save(party);

        if (!silent) {
            this.broadcast(party, "party.joined", "%player%", player.getName());
        }

        Bukkit.getPluginManager().callEvent(new PartyJoinEvent(party, player.getUniqueId()));
    }

    /**
     * Denies a leader's invite.
     *
     * @param player who denies
     * @param leaderName leader name
     * @return the result
     */
    public PartyResult deny(Player player, String leaderName) {
        final Party party = this.findInvitingParty(player.getUniqueId(), leaderName);

        if (party == null) {
            this.send(player, "party.invite.none", "%player%", leaderName);
            return PartyResult.NO_INVITE;
        }

        party.removeInvite(player.getUniqueId());

        this.send(player, "party.invite.denied", "%player%", party.getName(party.getLeader()));
        this.notifier.send(party.getLeader(), "party.invite.denied-sender", "%player%", player.getName());

        if (!this.disbandIfAlone(party)) {
            this.backend.save(party);
        }
        return PartyResult.SUCCESS;
    }

    private Party findInvitingParty(UUID player, String leaderName) {
        final UUID leader = this.lookup.findOnline(leaderName);
        final Party party = leader == null ? null : this.getParty(leader);

        return party != null && party.isLeader(leader) && party.isInvited(player) ? party : null;
    }

    /**
     * Leaves the party. If they were the leader, the next member takes over.
     *
     * @param player player
     * @return the result
     */
    public PartyResult leave(Player player) {
        final Party party = this.getParty(player);

        if (party == null) {
            this.send(player, "party.not-in-party");
            return PartyResult.NOT_IN_PARTY;
        }

        final boolean hidden = party.isHidden(player.getUniqueId());

        this.removeMember(party, player.getUniqueId(), PartyLeaveEvent.Reason.LEFT);
        this.send(player, "party.you-left");

        if (!hidden && this.getParty(party.getLeader()) != null) {
            this.broadcast(party, "party.left", "%player%", player.getName());
        }
        return PartyResult.SUCCESS;
    }

    /**
     * Kicks a member.
     *
     * @param leader leader
     * @param targetName member name
     * @return the result
     */
    public PartyResult kick(Player leader, String targetName) {
        final Party party = this.leaderParty(leader);

        if (party == null) {
            return PartyResult.NOT_LEADER;
        }

        final UUID target = party.findMember(targetName);

        if (target == null || target.equals(leader.getUniqueId())) {
            this.send(leader, "party.not-member", "%player%", targetName);
            return PartyResult.NOT_MEMBER;
        }

        final String name = party.getName(target);

        this.notifier.send(target, "party.kicked-you");
        this.removeMember(party, target, PartyLeaveEvent.Reason.KICKED);

        if (this.getParty(leader) != null) {
            this.broadcast(party, "party.kicked", "%player%", name);
        }
        return PartyResult.SUCCESS;
    }

    /**
     * Gives the lead to another member.
     *
     * @param leader current leader
     * @param targetName new leader name
     * @return the result
     */
    public PartyResult transfer(Player leader, String targetName) {
        final Party party = this.leaderParty(leader);

        if (party == null) {
            return PartyResult.NOT_LEADER;
        }

        final UUID target = party.findMember(targetName);

        if (target == null || target.equals(leader.getUniqueId())) {
            this.send(leader, "party.not-member", "%player%", targetName);
            return PartyResult.NOT_MEMBER;
        }

        this.changeLeader(party, target);
        this.broadcast(party, "party.promoted", "%player%", party.getName(target));
        return PartyResult.SUCCESS;
    }

    /**
     * Opens or closes the party for people without an invite.
     *
     * @param leader leader
     * @return the result
     */
    public PartyResult toggleOpen(Player leader) {
        final Party party = this.leaderParty(leader);

        if (party == null) {
            return PartyResult.NOT_LEADER;
        }

        party.setOpen(!party.isOpen());
        this.backend.save(party);
        this.broadcast(party, party.isOpen() ? "party.open.opened" : "party.open.closed", "%player%", leader.getName());
        return PartyResult.SUCCESS;
    }

    /**
     * Disbands the party.
     *
     * @param leader leader
     * @return the result
     */
    public PartyResult disband(Player leader) {
        final Party party = this.leaderParty(leader);

        if (party == null) {
            return PartyResult.NOT_LEADER;
        }

        this.broadcast(party, "party.disbanded");
        this.remove(party);
        return PartyResult.SUCCESS;
    }

    /**
     * Brings the members to wherever the leader is, through the handler set with {@link #setWarpHandler(BiFunction)}.
     *
     * @param leader leader
     * @return how many were moved, -1 if there's no handler or no party
     */
    public int warp(Player leader) {
        final Party party = this.leaderParty(leader);

        if (party == null || this.warpHandler == null) {
            return -1;
        }
        return this.warpHandler.apply(leader, party);
    }

    /**
     * Prints the members.
     *
     * @param player who asks
     */
    public void list(Player player) {
        final Party party = this.getParty(player);

        if (party == null) {
            this.send(player, "party.not-in-party");
            return;
        }

        this.send(player, "party.list.header", "%leader%", party.getName(party.getLeader()), "%size%", party.size(), "%max%", this.maxSize);

        for (UUID member : party.getMembers()) {
            if (party.isHidden(member) && !member.equals(player.getUniqueId())) {
                continue;
            }

            final boolean online = this.lookup.isOnline(member);

            this.send(player, party.isLeader(member) ? "party.list.leader" : "party.list.member",
                    "%status%", this.text(player, online ? "party.list.online" : "party.list.offline"),
                    "%level%", this.levelFormatter.apply(member),
                    "%player%", party.getName(member));
        }
    }

    /**
     * Sends a message to party chat.
     *
     * @param player who talks
     * @param message message
     */
    public void chat(Player player, String message) {
        final Party party = this.getParty(player);

        if (party == null) {
            this.send(player, "party.not-in-party");
            return;
        }

        final PartyChatEvent event = new PartyChatEvent(party, player, message);

        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        this.broadcast(party, "party.chat.format", "%level%", this.levelFormatter.apply(player.getUniqueId()), "%player%", player.getName(), "%message%", event.getMessage());
    }

    /**
     * Switches between normal chat and "everything goes to the party".
     *
     * @param player player
     */
    public void toggleChat(Player player) {
        if (this.chatToggled.remove(player.getUniqueId())) {
            this.send(player, "party.chat.toggled-off");
            return;
        }

        if (this.getParty(player) == null) {
            this.send(player, "party.not-in-party");
            return;
        }

        this.chatToggled.add(player.getUniqueId());
        this.send(player, "party.chat.toggled-on");
    }

    /**
     * Sends a language message to every member, wherever they are.
     *
     * @param party party
     * @param key language key
     * @param replacements placeholder and value pairs
     */
    public void broadcast(Party party, String key, Object... replacements) {
        for (UUID member : party.getMembers()) {
            this.notifier.send(member, key, replacements);
        }
    }

    private Party leaderParty(Player player) {
        final Party party = this.getParty(player);

        if (party == null) {
            this.send(player, "party.not-in-party");
            return null;
        }

        if (!party.isLeader(player.getUniqueId())) {
            this.send(player, "party.not-leader");
            return null;
        }
        return party;
    }

    private void removeMember(Party party, UUID uniqueId, PartyLeaveEvent.Reason reason) {
        final boolean wasLeader = party.isLeader(uniqueId);

        party.removeMember(uniqueId);
        this.backend.unlink(uniqueId);
        this.offlineSince.remove(uniqueId);
        this.chatToggled.remove(uniqueId);

        Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, uniqueId, reason));

        if (party.size() > 0 && wasLeader) {
            final UUID next = this.nextLeader(party);

            this.changeLeader(party, next);
            this.broadcast(party, "party.promoted", "%player%", party.getName(next));
        }

        if (!this.disbandIfAlone(party)) {
            this.backend.save(party);
        }
    }

    private void changeLeader(Party party, UUID leader) {
        party.setLeader(leader);
        this.backend.save(party);

        Bukkit.getPluginManager().callEvent(new PartyLeaderChangeEvent(party, leader));
    }

    private UUID nextLeader(Party party) {
        for (UUID member : party.getMembers()) {
            if (this.lookup.isOnline(member) && !party.isHidden(member)) {
                return member;
            }
        }
        return party.getMembers().get(0);
    }

    private boolean disbandIfAlone(Party party) {
        if (party.size() <= 1 && !party.hasPendingInvites()) {
            this.broadcast(party, "party.disbanded-alone");
            this.remove(party);
            return true;
        }
        return false;
    }

    private void remove(Party party) {
        for (UUID member : party.getMembers()) {
            this.offlineSince.remove(member);
            this.chatToggled.remove(member);
            this.backend.unlink(member);

            Bukkit.getPluginManager().callEvent(new PartyLeaveEvent(party, member, PartyLeaveEvent.Reason.DISBANDED));
        }

        this.backend.delete(party);

        Bukkit.getPluginManager().callEvent(new PartyDisbandEvent(party));
    }

    /**
     * Builds the text of a party message for a player, with the English fallback and the invite buttons.
     * Used by the local notifier and by remote servers when a message comes from the network.
     *
     * @param player receiver
     * @param key language key
     * @param replacements placeholder and value pairs
     * @return the text, may contain {@code {{...}}} fancy blocks
     */
    public String render(Player player, String key, Object... replacements) {
        if (key.equals("party.invite.buttons") && !this.hasKey("party.invite.buttons")) {
            final String leader = String.valueOf(replacements.length > 1 ? replacements[1] : "");

            return "{{" + this.text(player, "party.invite.accept") + "|run:/party accept " + leader + "|hover:" + this.text(player, "party.invite.accept-hover") + "}} "
                    + "{{" + this.text(player, "party.invite.deny") + "|run:/party deny " + leader + "|hover:" + this.text(player, "party.invite.deny-hover") + "}}";
        }
        return LanguageManager.replace(this.text(player, key), replacements);
    }

    private boolean hasKey(String key) {
        return Riven.get().getLanguage().has(key);
    }

    private String text(Player player, String key) {
        final String fallback = DEFAULTS.get(key);

        return Riven.get().getLanguage().getOrDefault(player, key, fallback != null ? fallback : key);
    }

    private void sendLocal(UUID member, String key, Object... replacements) {
        final Player player = Bukkit.getPlayer(member);

        if (player != null) {
            FancyMessage.sendAuto(player, this.render(player, key, replacements));
        }
    }

    private void send(Player player, String key, Object... replacements) {
        FancyMessage.sendAuto(player, this.render(player, key, replacements));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();

        if (!this.chatToggled.contains(player.getUniqueId()) || event.getMessage().startsWith("!")) {
            return;
        }

        event.setCancelled(true);

        final String message = event.getMessage();

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (this.getParty(player) == null) {
                this.chatToggled.remove(player.getUniqueId());
                player.chat(message);
                return;
            }
            this.chat(player, message);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (this.offlineSince.remove(player.getUniqueId()) != null) {
            final Party party = this.getParty(player);

            if (party != null && !party.isHidden(player.getUniqueId())) {
                this.broadcast(party, "party.reconnected", "%player%", player.getName());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final UUID uniqueId = event.getPlayer().getUniqueId();

        this.chatToggled.remove(uniqueId);

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            final Party party = this.getParty(uniqueId);

            if (party == null || this.lookup.isOnline(uniqueId)) {
                return;
            }

            this.offlineSince.put(uniqueId, System.currentTimeMillis());

            if (!party.isLeader(uniqueId)) {
                return;
            }

            for (UUID member : party.getMembers()) {
                if (!member.equals(uniqueId) && this.lookup.isOnline(member) && !party.isHidden(member)) {
                    this.changeLeader(party, member);
                    this.broadcast(party, "party.leader-offline", "%player%", party.getName(member));
                    return;
                }
            }
        }, 40L);
    }

    public PartyBackend getBackend() {
        return this.backend;
    }

    public void setBackend(PartyBackend backend) {
        this.backend = backend;
    }

    public PlayerLookup getLookup() {
        return this.lookup;
    }

    public void setLookup(PlayerLookup lookup) {
        this.lookup = lookup;
    }

    public PartyNotifier getNotifier() {
        return this.notifier;
    }

    /**
     * Swaps how messages reach members. Pass null to go back to "only players on this server".
     *
     * @param notifier notifier
     */
    public void setNotifier(PartyNotifier notifier) {
        this.notifier = notifier == null ? this::sendLocal : notifier;
    }

    /**
     * @param levelFormatter what replaces %level% in the list and chat (a level badge, a rank...)
     */
    public void setLevelFormatter(Function<UUID, String> levelFormatter) {
        this.levelFormatter = levelFormatter;
    }

    /**
     * @param warpHandler brings the party to the leader, returns how many were moved
     */
    public void setWarpHandler(BiFunction<Player, Party, Integer> warpHandler) {
        this.warpHandler = warpHandler;
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getInviteExpiration() {
        return this.inviteExpiration;
    }

    public void setInviteExpiration(int inviteExpiration) {
        this.inviteExpiration = inviteExpiration;
    }

    public int getOfflineTimeout() {
        return this.offlineTimeout;
    }

    public void setOfflineTimeout(int offlineTimeout) {
        this.offlineTimeout = offlineTimeout;
    }

}
