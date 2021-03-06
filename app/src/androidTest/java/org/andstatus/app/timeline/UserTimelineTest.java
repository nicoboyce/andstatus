/*
 * Copyright (C) 2017 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.timeline;

import android.content.Intent;

import org.andstatus.app.account.MyAccount;
import org.andstatus.app.activity.ActivityViewItem;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.andstatus.app.data.MatchedUri;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.OidEnum;
import org.andstatus.app.net.social.MbActivityType;
import org.andstatus.app.timeline.meta.Timeline;
import org.andstatus.app.timeline.meta.TimelineType;
import org.andstatus.app.util.MyLog;
import org.junit.Test;

import static org.andstatus.app.context.DemoData.demoData;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class UserTimelineTest extends TimelineActivityTest {

    @Override
    protected Intent getActivityIntent() {
        MyLog.i(this, "setUp started");
        TestSuite.initializeWithData(this);

        final MyAccount ma = demoData.getMyAccount(demoData.CONVERSATION_ACCOUNT_NAME);
        assertTrue(ma.isValid());
        MyContextHolder.get().persistentAccounts().setCurrentAccount(ma);
        long userId = MyQuery.oidToId(OidEnum.USER_OID, ma.getOriginId(), demoData.CONVERSATION_AUTHOR_SECOND_USER_OID);

        MyLog.i(this, "setUp ended");
        final Timeline timeline = Timeline.getTimeline(TimelineType.USER, ma, userId, ma.getOrigin());
        timeline.forgetPositionsAndDates();
        return new Intent(Intent.ACTION_VIEW, MatchedUri.getTimelineUri(timeline));
    }

    @Test
    public void openSecondAuthorTimeline() throws InterruptedException {
        final String method = "openSecondAuthorTimeline";
        TestSuite.waitForListLoaded(getActivity(), 10);
        TimelineData<ActivityViewItem> timelineData = getActivity().getListData();
        ActivityViewItem followItem = ActivityViewItem.EMPTY;
        for (int position = 0; position < timelineData.size(); position++) {
            ActivityViewItem item = timelineData.getItem(position);
            if (item.activityType == MbActivityType.FOLLOW) {
                followItem = item;
            }
        }
        assertNotEquals("No follow action by " + demoData.CONVERSATION_AUTHOR_SECOND_USER_OID
                + " in " + timelineData,
                ActivityViewItem.EMPTY, followItem);
    }
}
