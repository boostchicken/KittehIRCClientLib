/*
 * * Copyright (C) 2013-2015 Matt Baxter http://kitteh.org
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kitteh.irc.client.library.implementation;

import org.kitteh.irc.client.library.CaseMapping;
import org.kitteh.irc.client.library.ChannelModeType;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.ServerInfo;
import org.kitteh.irc.client.library.element.ChannelUserMode;
import org.kitteh.irc.client.library.util.Sanity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class IRCServerInfo implements ServerInfo {
    private CaseMapping caseMapping = CaseMapping.RFC1459;
    private int channelLengthLimit = -1;
    private Map<Character, Integer> channelLimits = new HashMap<>();
    private Map<Character, ChannelModeType> channelModes = ChannelModeType.getDefaultModes();
    private List<Character> channelPrefixes = Arrays.asList('#', '&', '!', '+');
    private List<ChannelUserMode> channelUserModes;
    private List<String> motd;
    private String networkName;
    private int nickLengthLimit = -1;
    private String address;
    private String version;
    private boolean supportsWhoX;

    // TODO adapt for changes
    // Pattern: ([#!&\+][^ ,\07\r\n]{1,49})
    // Screw it, let's assume IRCDs disregard length policy
    // New pattern: ([#!&\+][^ ,\07\r\n]+)
    private final Pattern channelPattern = Pattern.compile("([#!&\\+][^ ,\\07\\r\\n]+)");

    IRCServerInfo(@Nonnull Client client) {
        this.channelUserModes = new ArrayList<>();
        this.channelUserModes.add(new ActorProvider.IRCChannelUserMode(client, 'o', '@'));
        this.channelUserModes.add(new ActorProvider.IRCChannelUserMode(client, 'v', '+'));
    }

    @Nullable
    @Override
    public String getAddress() {
        return this.address;
    }

    void setAddress(@Nonnull String serverAddress) {
        this.address = serverAddress;
    }

    @Nonnull
    @Override
    public CaseMapping getCaseMapping() {
        return this.caseMapping;
    }

    void setCaseMapping(@Nonnull CaseMapping caseMapping) {
        this.caseMapping = caseMapping;
    }

    @Override
    public int getChannelLengthLimit() {
        return this.channelLengthLimit;
    }

    void setChannelLengthLimit(int channelLengthLimit) {
        this.channelLengthLimit = channelLengthLimit;
    }

    @Nonnull
    @Override
    public Map<Character, Integer> getChannelLimits() {
        return new HashMap<>(this.channelLimits);
    }

    void setChannelLimits(Map<Character, Integer> channelLimits) {
        this.channelLimits = channelLimits;
    }

    @Nonnull
    @Override
    public Map<Character, ChannelModeType> getChannelModes() {
        return new HashMap<>(this.channelModes);
    }

    void setChannelModes(Map<Character, ChannelModeType> channelModes) {
        this.channelModes = channelModes;
    }

    @Nonnull
    @Override
    public List<Character> getChannelPrefixes() {
        return new ArrayList<>(this.channelPrefixes);
    }

    void setChannelPrefixes(@Nonnull List<Character> channelPrefixes) {
        this.channelPrefixes = channelPrefixes;
    }

    @Nonnull
    @Override
    public List<ChannelUserMode> getChannelUserModes() {
        return new ArrayList<>(this.channelUserModes);
    }

    void setChannelUserModes(@Nonnull List<ChannelUserMode> channelUserModes) {
        this.channelUserModes = channelUserModes;
    }

    @Nonnull
    @Override
    public List<String> getMOTD() {
        return this.motd;
    }

    void setMOTD(@Nonnull List<String> motd) {
        this.motd = Collections.unmodifiableList(motd);
    }

    @Nullable
    @Override
    public String getNetworkName() {
        return this.networkName;
    }

    void setNetworkName(@Nonnull String networkName) {
        this.networkName = networkName;
    }

    @Override
    public int getNickLengthLimit() {
        return this.nickLengthLimit;
    }

    void setNickLengthLimit(int nickLengthLimit) {
        this.nickLengthLimit = nickLengthLimit;
    }

    @Nullable
    @Override
    public String getVersion() {
        return this.version;
    }

    void setVersion(@Nonnull String version) {
        this.version = version;
    }

    @Override
    public boolean hasWhoXSupport() {
        return this.supportsWhoX;
    }

    void setWhoXSupport() {
        this.supportsWhoX = true;
    }

    // Util stuffs
    @Override
    public boolean isValidChannel(@Nonnull String name) {
        Sanity.nullCheck(name, "Name cannot be null");
        return (name.length() > 1) && ((this.channelLengthLimit < 0) || ((name.length() <= this.channelLengthLimit) && this.getChannelPrefixes().contains(name.charAt(0)) && this.channelPattern.matcher(name).matches()));
    }

    boolean isTargetedChannel(@Nonnull String name) {
        return this.getTargetedChannelInfo(name) != null;
    }

    @Nullable
    ChannelUserMode getTargetedChannelInfo(@Nonnull String name) {
        final char first = name.charAt(0);
        final String shorter = name.substring(1);
        if (!this.channelPrefixes.contains(first) && this.isValidChannel(shorter)) {
            for (ChannelUserMode mode : this.channelUserModes) {
                if (mode.getPrefix() == first) {
                    return mode;
                }
            }
        }
        return null;
    }
}