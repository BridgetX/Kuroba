/*
 * Kuroba - *chan browser https://github.com/Adamantcheese/Kuroba/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.adamantcheese.chan.ui.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.adamantcheese.chan.R;
import com.github.adamantcheese.chan.ui.theme.ThemeHelper;
import com.github.adamantcheese.chan.utils.Logger;

import java.util.List;

import static com.github.adamantcheese.chan.utils.AndroidUtils.dp;
import static com.github.adamantcheese.chan.utils.LayoutUtils.inflate;
import static com.github.adamantcheese.chan.utils.LayoutUtils.measureContentWidth;

public class FloatingMenu<T> {
    private final Context context;
    private View anchor;
    private int anchorGravity = Gravity.RIGHT;
    private int anchorOffsetX = -dp(5);
    private int anchorOffsetY = dp(5);
    private int popupHeight = -1;
    private List<FloatingMenuItem<T>> items;
    private FloatingMenuItem<T> selectedItem;
    private ListAdapter adapter;
    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;

    private ListPopupWindow popupWindow;
    private FloatingMenuCallback<T> callback;

    public FloatingMenu(Context context, @NonNull View anchor, @NonNull List<FloatingMenuItem<T>> items) {
        this.context = context;
        this.anchor = anchor;
        this.items = items;
    }

    public void setAnchorGravity(int anchorGravity, int anchorOffsetX, int anchorOffsetY) {
        this.anchorGravity = anchorGravity;
        this.anchorOffsetX = anchorOffsetX;
        this.anchorOffsetY = anchorOffsetY;
    }

    public void setPopupHeight(int height) {
        this.popupHeight = height;
        if (popupWindow != null) {
            popupWindow.setHeight(height);
        }
    }

    public void setSelectedItem(FloatingMenuItem<T> item) {
        this.selectedItem = item;
    }

    public void setAdapter(ListAdapter adapter) {
        this.adapter = adapter;
        if (popupWindow != null) {
            popupWindow.setAdapter(adapter);
        }
    }

    public void setCallback(FloatingMenuCallback<T> callback) {
        this.callback = callback;
    }

    public void show() {
        popupWindow = new ListPopupWindow(context);
        popupWindow.setAnchorView(anchor);
        popupWindow.setModal(true);
        popupWindow.setDropDownGravity(anchorGravity);
        popupWindow.setVerticalOffset(-anchor.getHeight() + anchorOffsetY);
        popupWindow.setHorizontalOffset(anchorOffsetX);

        if (popupHeight > 0) {
            popupWindow.setHeight(popupHeight);
        }

        int selection = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == selectedItem) {
                selection = i;
            }
        }

        if (adapter != null) {
            popupWindow.setAdapter(adapter);
            popupWindow.setWidth(measureContentWidth(context, adapter, dp(3 * 56)));
        } else {
            FloatingMenuArrayAdapter<T> arrayAdapter = new FloatingMenuArrayAdapter<>(context,
                    com.github.adamantcheese.chan.R.layout.toolbar_menu_item,
                    items
            );
            popupWindow.setAdapter(arrayAdapter);
            popupWindow.setWidth(measureContentWidth(context, arrayAdapter, dp(3 * 56)));
        }

        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < items.size()) {
                FloatingMenuItem<T> item = items.get(position);
                callback.onFloatingMenuItemClicked(FloatingMenu.this, item);
                dismiss();
            } else {
                callback.onFloatingMenuItemClicked(FloatingMenu.this, null);
            }
        });

        globalLayoutListener = () -> {
            if (popupWindow == null) {
                Logger.d(FloatingMenu.this, "popupWindow null in layout listener");
            } else {
                if (popupWindow.isShowing()) {
                    // Recalculate anchor position
                    popupWindow.show();
                }
            }
        };
        anchor.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);

        popupWindow.setOnDismissListener(() -> {
            if (anchor.getViewTreeObserver().isAlive()) {
                anchor.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            }
            globalLayoutListener = null;
            popupWindow = null;
            callback.onFloatingMenuDismissed(FloatingMenu.this);
        });

        popupWindow.show();
        popupWindow.setSelection(selection);
    }

    public boolean isShowing() {
        return popupWindow != null && popupWindow.isShowing();
    }

    public void dismiss() {
        if (isShowing()) {
            popupWindow.dismiss();
            popupWindow = null;
        }
    }

    public interface FloatingMenuCallback<R> {
        void onFloatingMenuItemClicked(FloatingMenu<R> menu, FloatingMenuItem<R> item);

        void onFloatingMenuDismissed(FloatingMenu<R> menu);
    }

    public abstract static class ClickCallback<S>
            implements FloatingMenuCallback<S> {

        @Override
        public abstract void onFloatingMenuItemClicked(FloatingMenu<S> menu, FloatingMenuItem<S> item);

        @Override
        public final void onFloatingMenuDismissed(FloatingMenu<S> menu) {}
    }

    private static class FloatingMenuArrayAdapter<T>
            extends ArrayAdapter<FloatingMenuItem<T>> {
        public FloatingMenuArrayAdapter(Context context, int resource, List<FloatingMenuItem<T>> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflate(parent.getContext(), R.layout.toolbar_menu_item, parent, false);
            }

            FloatingMenuItem<T> item = getItem(position);

            TextView textView = (TextView) convertView;
            textView.setText(item.getText());
            textView.setTypeface(ThemeHelper.getTheme().mainFont);

            return textView;
        }
    }
}
