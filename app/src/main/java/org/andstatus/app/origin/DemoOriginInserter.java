package org.andstatus.app.origin;

import org.andstatus.app.account.AccountName;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContext;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.net.http.HttpConnectionData;
import org.andstatus.app.net.http.OAuthClientKeys;
import org.andstatus.app.net.http.SslModeEnum;
import org.andstatus.app.timeline.meta.Timeline;
import org.andstatus.app.timeline.meta.TimelineType;
import org.andstatus.app.util.TriState;
import org.andstatus.app.util.UrlUtils;

import static org.andstatus.app.context.DemoData.demoData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class DemoOriginInserter {
    private MyContext myContext;

    public DemoOriginInserter(MyContext myContext) {
        this.myContext = myContext;
    }

    public void insert() {
        demoData.checkDataPath();
        createOneOrigin(OriginType.TWITTER, demoData.TWITTER_TEST_ORIGIN_NAME,
                demoData.getTestOriginHost(demoData.TWITTER_TEST_ORIGIN_NAME),
                true, SslModeEnum.SECURE, false, true, true);
        createOneOrigin(OriginType.PUMPIO,
                demoData.PUMPIO_ORIGIN_NAME,
                demoData.getTestOriginHost(demoData.PUMPIO_ORIGIN_NAME),
                true, SslModeEnum.SECURE, true, true, true);
        createOneOrigin(OriginType.GNUSOCIAL, demoData.GNUSOCIAL_TEST_ORIGIN_NAME,
                demoData.getTestOriginHost(demoData.GNUSOCIAL_TEST_ORIGIN_NAME),
                true, SslModeEnum.SECURE, true, true, true);
        String additionalOriginName = demoData.GNUSOCIAL_TEST_ORIGIN_NAME + "ins";
        createOneOrigin(OriginType.GNUSOCIAL, additionalOriginName,
                demoData.getTestOriginHost(additionalOriginName),
                true, SslModeEnum.INSECURE, true, false, true);
        createOneOrigin(OriginType.MASTODON, demoData.MASTODON_TEST_ORIGIN_NAME,
                demoData.getTestOriginHost(demoData.MASTODON_TEST_ORIGIN_NAME),
                true, SslModeEnum.SECURE, true, true, true);
        myContext.persistentOrigins().initialize();
    }

    Origin createOneOrigin(OriginType originType,
                                                 String originName, String hostOrUrl, boolean isSsl,
                                                 SslModeEnum sslMode, boolean allowHtml,
                                                 boolean inCombinedGlobalSearch, boolean inCombinedPublicReload) {
        Origin.Builder builder = new Origin.Builder(originType).setName(originName)
                .setHostOrUrl(hostOrUrl)
                .setSsl(isSsl)
                .setSslMode(sslMode)
                .setHtmlContentAllowed(allowHtml)
                .save();
        assertTrue(builder.toString(), builder.isSaved());
        Origin origin = builder.build();
        if (origin.isOAuthDefault() || origin.canChangeOAuth()) {
            insertTestKeys(origin);
        }

        checkAttributes(origin, originName, hostOrUrl, isSsl, sslMode, allowHtml,
                inCombinedGlobalSearch, inCombinedPublicReload);

        MyContextHolder.get().persistentOrigins().initialize();
        Origin origin2 = MyContextHolder.get().persistentOrigins()
                .fromId(origin.getId());
        checkAttributes(origin2, originName, hostOrUrl, isSsl, sslMode, allowHtml,
                inCombinedGlobalSearch, inCombinedPublicReload);
        return origin;
    }

    private void checkAttributes(Origin origin, String originName, String hostOrUrl,
                                        boolean isSsl, SslModeEnum sslMode, boolean allowHtml, boolean inCombinedGlobalSearch, boolean inCombinedPublicReload) {
        assertTrue("Origin " + originName + " added", origin.isPersistent());
        assertEquals(originName, origin.getName());
        if (origin.canSetUrlOfOrigin()) {
            if (UrlUtils.isHostOnly(UrlUtils.buildUrl(hostOrUrl, isSsl))) {
                assertEquals((isSsl ? "https" : "http") + "://" + hostOrUrl,
                        origin.getUrl().toExternalForm());
            } else {
                String hostOrUrl2 = hostOrUrl.endsWith("/") ? hostOrUrl
                        : hostOrUrl + "/";
                assertEquals("Input host or URL: '" + hostOrUrl + "'",
                        UrlUtils.buildUrl(hostOrUrl2, origin.isSsl()),
                        origin.getUrl());
            }
        } else {
            assertEquals(origin.getOriginType().getUrlDefault(), origin.getUrl());
        }
        assertEquals(isSsl, origin.isSsl());
        assertEquals(sslMode, origin.getSslMode());
        assertEquals(allowHtml, origin.isHtmlContentAllowed());
    }


    private void insertTestKeys(Origin origin) {
        HttpConnectionData connectionData = HttpConnectionData.fromConnectionData(
                OriginConnectionData.fromAccountName(
                        AccountName.fromOriginAndUserName(origin, ""), TriState.UNKNOWN)
        );
        final String consumerKey = "testConsumerKey" + Long.toString(System.nanoTime());
        final String consumerSecret = "testConsumerSecret" + Long.toString(System.nanoTime());
        if (connectionData.originUrl == null) {
            connectionData.originUrl = UrlUtils.fromString("https://" + demoData.PUMPIO_MAIN_HOST);
        }
        OAuthClientKeys keys1 = OAuthClientKeys.fromConnectionData(connectionData);
        if (!keys1.areKeysPresent()) {
            keys1.setConsumerKeyAndSecret(consumerKey, consumerSecret);
            // Checking
            OAuthClientKeys keys2 = OAuthClientKeys.fromConnectionData(connectionData);
            assertEquals("Keys are loaded for " + origin, true, keys2.areKeysPresent());
            assertEquals(consumerKey, keys2.getConsumerKey());
            assertEquals(consumerSecret, keys2.getConsumerSecret());
        }
    }

    public void checkDefaultTimelinesForOrigins() {
        for (Origin origin : MyContextHolder.get().persistentOrigins().collection()) {
            MyAccount myAccount = MyContextHolder.get().persistentAccounts().
                    getFirstSucceededForOrigin(origin);
            for (TimelineType timelineType : TimelineType.getDefaultOriginTimelineTypes()) {
                int count = 0;
                for (Timeline timeline : MyContextHolder.get().persistentTimelines().values()) {
                    if (timeline.getOrigin().equals(origin) &&
                            timeline.getTimelineType().equals(timelineType) &&
                            timeline.getSearchQuery().isEmpty()) {
                        count++;
                    }
                }
                if (myAccount.isValid() && origin.getOriginType().isTimelineTypeSyncable(timelineType)) {
                    assertTrue(origin.toString() + " " + timelineType, count > 0);
                }
            }
        }
    }
}
