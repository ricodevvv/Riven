package dev.ricodevvv.riven.party;

/**
 * How a party action went. Anything that isn't {@link #SUCCESS} already told the player why.
 */
public enum PartyResult {

    SUCCESS,
    SELF,
    NOT_IN_PARTY,
    NOT_LEADER,
    ALREADY_MEMBER,
    TARGET_IN_PARTY,
    ALREADY_IN_PARTY,
    ALREADY_INVITED,
    NO_INVITE,
    NOT_MEMBER,
    FULL,
    CLOSED,
    CANCELLED

}
