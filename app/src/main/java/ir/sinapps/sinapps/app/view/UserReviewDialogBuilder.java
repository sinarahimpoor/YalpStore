/*
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package ir.sinapps.sinapps.app.view;

import android.content.DialogInterface;
import android.widget.EditText;

import ir.sinapps.sinapps.app.R;
import ir.sinapps.sinapps.app.YalpStoreActivity;
import ir.sinapps.sinapps.app.fragment.details.Review;
import ir.sinapps.sinapps.app.task.playstore.ReviewAddTask;

public class UserReviewDialogBuilder extends DialogWrapper {

    private Review manager;
    private String packageName;

    public UserReviewDialogBuilder(YalpStoreActivity activity, Review manager, String packageName) {
        super(activity);
        this.manager = manager;
        this.packageName = packageName;
    }

    public DialogWrapper show(final ir.sinapps.sinapps.app.model.Review review) {
        setLayout(R.layout.review_dialog_layout);

        getCommentView().setText(review.getComment());
        getTitleView().setText(review.getTitle());

        setCancelable(true);
        setTitle(R.string.details_review_dialog_title);
        setPositiveButton(android.R.string.ok, new DoneOnClickListener(review));
        setNegativeButton(android.R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        show();
        return this;
    }

    private EditText getCommentView() {
        return (EditText) findViewById(R.id.review_dialog_review_comment);
    }

    private EditText getTitleView() {
        return (EditText) findViewById(R.id.review_dialog_review_title);
    }

    private class DoneOnClickListener implements DialogInterface.OnClickListener {

        private final ir.sinapps.sinapps.app.model.Review review;

        public DoneOnClickListener(ir.sinapps.sinapps.app.model.Review review) {
            this.review = review;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            ReviewAddTask task = new ReviewAddTask();
            task.setContext(activity);
            task.setPackageName(packageName);
            task.setFragment(manager);
            review.setComment(getCommentView().getText().toString());
            review.setTitle(getTitleView().getText().toString());
            task.setReview(review);
            task.execute();
            dismiss();
        }
    }
}
