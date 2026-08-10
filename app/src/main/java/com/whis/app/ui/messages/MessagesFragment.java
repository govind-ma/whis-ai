package com.whis.app.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whis.app.R;
import com.whis.app.core.DetectionResult;
import com.whis.app.ui.components.ListAnimationHelper;
import com.whis.app.ui.components.WhisSwipeActionCallback;
import com.whis.app.ui.home.ActivityFeedAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * MessagesFragment — Message Protection module UI (UI_PLAN.md §2.4 / §3.5).
 * <p>
 * Displays AI-analysed SMS alerts screened since protection was turned on.
 * Starts empty on fresh install; the {@link com.whis.app.msg.SmsFilterService}
 * populates data as real SMS arrive and are analysed by Gemini.
 * <p>
 * Animation layers:
 * <ul>
 *   <li>Staggered slide-up entrance on first RecyclerView load (cap=5, 60ms stagger)</li>
 *   <li>Swipe left = "Mark Safe" (green), swipe right = "Report Scam" (red) via
 *       {@link WhisSwipeActionCallback}; partial swipes spring back</li>
 *   <li>Empty state with floating shield icon when list is empty</li>
 * </ul>
 */
public class MessagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Top Bar 3-Dot Overflow Menu Handler
        View btnMenu = view.findViewById(R.id.btn_messages_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), v);
                popup.getMenu().add("🗑️ Clear Message Log");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().toString().contains("Clear Message Log")) {
                        new Thread(() -> {
                            try {
                                com.whis.app.msg.storage.LocalMsgDatabase.getInstance(requireContext()).msgHistoryDao().clearAll();
                            } catch (Exception ignored) {}
                        }).start();
                        Toast.makeText(requireContext(), "Message log cleared", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // Feed populated by real SMS entries from LocalMsgDatabase
        List<DetectionResult> msgItems = new ArrayList<>();
        try {
            List<com.whis.app.msg.storage.MsgHistoryEntry> smsEntries =
                    com.whis.app.msg.storage.LocalMsgDatabase.getInstance(requireContext())
                            .msgHistoryDao().getRecentVerdicts();

            for (com.whis.app.msg.storage.MsgHistoryEntry sms : smsEntries) {
                com.whis.app.msg.model.MsgDetectionResult res = new com.whis.app.msg.model.MsgDetectionResult();
                res.sender = sms.sender;
                res.threatLevel = sms.threat_level != null ? sms.threat_level : "SAFE";
                res.riskScore = (int) (sms.confidence * 100);
                res.reasonText = sms.sender + " - " + (sms.reason_text != null ? sms.reason_text : "SMS screened");
                res.timestamp = sms.timestamp;
                res.identifierType = "UNKNOWN_MOBILE";
                res.category = com.whis.app.msg.model.MsgCategory.GENERAL;
                res.resolveVerdict(false);
                msgItems.add(res);
            }
        } catch (Exception e) {
            // Defensive
        }

        // ── Empty state ──────────────────────────────────────────────────────
        View emptyState = view.findViewById(R.id.messages_empty_state);
        View emptyIcon  = emptyState.findViewById(R.id.empty_state_icon);
        if (msgItems.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            ListAnimationHelper.startIconFloat(emptyIcon);
        } else {
            emptyState.setVisibility(View.GONE);
        }

        // ── Screened Messages RecyclerView ───────────────────────────────────
        RecyclerView rvFeed = view.findViewById(R.id.rv_messages_feed);
        rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));

        final ActivityFeedAdapter[] adapterHolder = new ActivityFeedAdapter[1];
        ActivityFeedAdapter adapter = new ActivityFeedAdapter(msgItems, (item, position) -> {
            com.whis.app.ui.alert.AlertRenderer.showBottomSheetAlert(requireContext(), item, new com.whis.app.ui.alert.AlertRenderer.AlertActionListener() {
                @Override
                public void onPrimaryAction(String phoneNumber) {
                    if (item.getVerdict() == com.whis.app.core.WhisVerdict.HIGH_RISK) {
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            com.whis.app.call.BlockedNumberStore.block(requireContext(), phoneNumber);
                            Toast.makeText(requireContext(),
                                    "🚫 Blocked: " + phoneNumber, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "🚫 Blocked high-risk message", Toast.LENGTH_SHORT).show();
                        }
                        msgItems.remove(position);
                        if (adapterHolder[0] != null) adapterHolder[0].notifyItemRemoved(position);
                    }
                }

                @Override
                public void onSecondaryAction() {
                    com.whis.app.agent.AgentLauncher.launch(requireContext());
                }
            });
        });
        adapterHolder[0] = adapter;

        // ── Animation Layer 1: staggered entrance observer ───────────────────
        adapter.registerAdapterDataObserver(
                ListAnimationHelper.staggeredEntranceObserver(rvFeed));

        rvFeed.setAdapter(adapter);

        // ── Animation Layer 2: swipe actions ─────────────────────────────────
        WhisSwipeActionCallback.attachTo(rvFeed, new WhisSwipeActionCallback.SwipeListener() {
            @Override
            public void onMarkSafe(int adapterPosition) {
                if (adapterPosition < msgItems.size()) {
                    DetectionResult item = msgItems.get(adapterPosition);
                    Toast.makeText(requireContext(), "✅ Marked safe", Toast.LENGTH_SHORT).show();
                    msgItems.remove(adapterPosition);
                    adapter.notifyItemRemoved(adapterPosition);
                    if (msgItems.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        ListAnimationHelper.startIconFloat(emptyIcon);
                    }
                }
            }

            @Override
            public void onReportScam(int adapterPosition) {
                if (adapterPosition < msgItems.size()) {
                    DetectionResult item = msgItems.get(adapterPosition);
                    String sender = null;
                    if (item instanceof com.whis.app.msg.model.MsgDetectionResult) {
                        sender = ((com.whis.app.msg.model.MsgDetectionResult) item).sender;
                    }

                    // Block sender number persistently
                    if (sender != null && !sender.isEmpty()) {
                        com.whis.app.call.BlockedNumberStore.block(requireContext(), sender);
                        Toast.makeText(requireContext(), "🚫 Reported & Blocked: " + sender, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "🚫 Reported scam message", Toast.LENGTH_SHORT).show();
                    }

                    msgItems.remove(adapterPosition);
                    adapter.notifyItemRemoved(adapterPosition);
                    if (msgItems.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        ListAnimationHelper.startIconFloat(emptyIcon);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View root = getView();
        if (root != null) {
            View emptyState = root.findViewById(R.id.messages_empty_state);
            if (emptyState != null) {
                View icon = emptyState.findViewById(R.id.empty_state_icon);
                if (icon != null) ListAnimationHelper.stopIconFloat(icon);
            }
        }
    }
}
