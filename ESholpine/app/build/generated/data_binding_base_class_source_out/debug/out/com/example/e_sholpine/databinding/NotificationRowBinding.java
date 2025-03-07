// Generated by view binder compiler. Do not edit!
package com.example.e_sholpine.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.e_sholpine.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class NotificationRowBinding implements ViewBinding {
  @NonNull
  private final RelativeLayout rootView;

  @NonNull
  public final TextView txvContent;

  @NonNull
  public final TextView txvTime;

  @NonNull
  public final TextView txvUsername;

  private NotificationRowBinding(@NonNull RelativeLayout rootView, @NonNull TextView txvContent,
      @NonNull TextView txvTime, @NonNull TextView txvUsername) {
    this.rootView = rootView;
    this.txvContent = txvContent;
    this.txvTime = txvTime;
    this.txvUsername = txvUsername;
  }

  @Override
  @NonNull
  public RelativeLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static NotificationRowBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static NotificationRowBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.notification_row, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static NotificationRowBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.txvContent;
      TextView txvContent = ViewBindings.findChildViewById(rootView, id);
      if (txvContent == null) {
        break missingId;
      }

      id = R.id.txvTime;
      TextView txvTime = ViewBindings.findChildViewById(rootView, id);
      if (txvTime == null) {
        break missingId;
      }

      id = R.id.txvUsername;
      TextView txvUsername = ViewBindings.findChildViewById(rootView, id);
      if (txvUsername == null) {
        break missingId;
      }

      return new NotificationRowBinding((RelativeLayout) rootView, txvContent, txvTime,
          txvUsername);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
