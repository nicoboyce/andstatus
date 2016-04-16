/*
 * Copyright (c) 2015 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.msg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.andstatus.app.R;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.MyPreferences;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.graphics.MyImageCache;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.MyUrlSpan;
import org.andstatus.app.widget.MyBaseAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yvolk@yurivolkov.com
 */
public class TimelineAdapter extends MyBaseAdapter {
    private final MessageContextMenu contextMenu;
    private final int listItemLayoutId;
    private final TimelinePages pages;
    private final boolean showAvatars = MyPreferences.showAvatars();
    private final boolean showAttachedImages = MyPreferences.showAttachedImages();
    private final boolean showButtonsBelowMessages =
            MyPreferences.getBoolean(MyPreferences.KEY_SHOW_BUTTONS_BELOW_MESSAGE, true);
    private final boolean markReplies = MyPreferences.getBoolean(
            MyPreferences.KEY_MARK_REPLIES_IN_TIMELINE, false);
    private int positionPrev = -1;
    private Set<Long> preloadedImages = new HashSet<>(100);

    public TimelineAdapter(MessageContextMenu contextMenu, int listItemLayoutId,
                           TimelineAdapter oldAdapter, TimelinePage loadedPage) {
        this.contextMenu = contextMenu;
        this.listItemLayoutId = listItemLayoutId;
        this.pages = new TimelinePages( oldAdapter == null ? null : oldAdapter.getPages(), loadedPage);
    }

    @Override
    public int getCount() {
        return pages.getItemsCount();
    }

    @Override
    public TimelineViewItem getItem(View view) {
        return (TimelineViewItem) super.getItem(view);
    }

    @Override
    public TimelineViewItem getItem(int position) {
        return pages.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).msgId;
    }

    public TimelinePages getPages() {
        return pages;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView == null ? newView() : convertView;
        view.setOnCreateContextMenuListener(contextMenu);
        view.setOnClickListener(this);
        setPosition(view, position);
        TimelineViewItem item = getItem(position);
        MyUrlSpan.showText(view, R.id.message_author, item.authorName, false);
        showMessageBody(item, view);
        MyUrlSpan.showText(view, R.id.message_details, item.getDetails(contextMenu.getActivity()), false);
        if (showAvatars) {
            showAvatar(item, view);
        }
        if (showAttachedImages) {
            showAttachedImage(item, view);
        }
        showFavorited(item, view);
        if (markReplies) {
            showMarkReplies(item, view);
        }
        if (showButtonsBelowMessages) {
            showButtonsBelowMessage(item, view);
        }
        preloadAttachments(position);
        positionPrev = position;
        return view;
    }

    private void showMessageBody(TimelineViewItem item, View messageView) {
        TextView body = (TextView) messageView.findViewById(R.id.message_body);
        MyUrlSpan.showText(body, item.body, true);
        body.setOnClickListener(this);
    }

    private void preloadAttachments(int position) {
        if (positionPrev < 0 || position == positionPrev) {
            return;
        }
        Integer positionToPreload = position;
        for (int i = 0; i < 5; i++) {
            positionToPreload = positionToPreload + (position > positionPrev ? 1 : -1);
            if (positionToPreload < 0 || positionToPreload >= pages.getItemsCount()) {
                break;
            }
            TimelineViewItem item = getItem(positionToPreload);
            if (!preloadedImages.contains(item.msgId)) {
                preloadedImages.add(item.msgId);
                item.getAttachedImageFile().preloadAttachedImage(contextMenu.messageList);
                break;
            }
        }
    }

    private View newView() {
        View view = LayoutInflater.from(contextMenu.getActivity()).inflate(listItemLayoutId, null);
        if (showButtonsBelowMessages) {
            View buttons = view.findViewById(R.id.message_buttons);
            buttons.setVisibility(View.VISIBLE);
            buttons.findViewById(R.id.reply_button).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onButtonClick(v, MessageListContextMenuItem.REPLY);
                        }
                    }
            );
            buttons.findViewById(R.id.reblog_button).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onButtonClick(v, MessageListContextMenuItem.REBLOG);
                        }
                    }
            );
            buttons.findViewById(R.id.favorite_button).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onButtonClick(v, MessageListContextMenuItem.FAVORITE);
                        }
                    }
            );
        }
        return view;
    }

    private void onButtonClick(View v, MessageListContextMenuItem contextMenuItemIn) {
        TimelineViewItem item = getItem(v);
        if (item != null && item.msgStatus == DownloadStatus.LOADED && item.linkedUserId != 0) {
            MessageListContextMenuItem contextMenuItem = contextMenuItemIn;
            if (contextMenuItem == MessageListContextMenuItem.FAVORITE
                    && item.favorited) {
                contextMenuItem = MessageListContextMenuItem.DESTROY_FAVORITE;
            }
            contextMenu.onContextMenuItemSelected(contextMenuItem, item.msgId, item.linkedUserId);
        }
    }

    private void showAvatar(TimelineViewItem item, View view) {
        ImageView avatar = (ImageView) view.findViewById(R.id.avatar_image);
        avatar.setImageDrawable(item.getAvatar());
    }

    private void showAttachedImage(TimelineViewItem item, View view) {
        preloadedImages.add(item.msgId);
        item.getAttachedImageFile().showAttachedImage(contextMenu.messageList,
                (ImageView) view.findViewById(R.id.attached_image));
    }

    private void showFavorited(TimelineViewItem item, View view) {
        View favorited = view.findViewById(R.id.message_favorited);
        favorited.setVisibility(item.favorited ? View.VISIBLE : View.GONE );
    }

    private void showMarkReplies(TimelineViewItem item, View view) {
        if (item.inReplyToUserId != 0 && MyContextHolder.get().persistentAccounts().
                fromUserId(item.inReplyToUserId).isValid()) {
            // For some reason, referring to the style drawable doesn't work
            // (to "?attr:replyBackground" )
            view.setBackground( MyImageCache.getStyledDrawable(
                    R.drawable.reply_timeline_background_light,
                    R.drawable.reply_timeline_background));
        } else {
            view.setBackgroundResource(0);
            view.setPadding(0, 0, 0, 0);
        }
    }

    private void showButtonsBelowMessage(TimelineViewItem item, View view) {
        View buttons = view.findViewById(R.id.message_buttons);
        if (showButtonsBelowMessages && item.msgStatus == DownloadStatus.LOADED) {
            buttons.setVisibility(View.VISIBLE);
            ImageView imageView = (ImageView) buttons.findViewById(R.id.favorite_button);
            imageView.setAlpha(item.favorited ? 1f : 0.5f );
        } else {
            buttons.setVisibility(View.GONE);
        }
    }

    @Override
    public String toString() {
        return MyLog.formatKeyValue(this, pages);
    }

    @Override
    public void onClick(View v) {
        boolean handled = false;
        if (MyPreferences.isLongPressToOpenContextMenu()) {
            TimelineViewItem item = getItem(v);
            if (TimelineActivity.class.isAssignableFrom(contextMenu.messageList.getClass())) {
                ((TimelineActivity) contextMenu.messageList).onItemClick(item);
                handled = true;
            }
        }
        if (!handled) {
            super.onClick(v);
        }
    }
}
