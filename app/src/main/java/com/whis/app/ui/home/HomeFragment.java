package com.whis.app.ui.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whis.app.R;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;
import com.whis.app.ui.components.ListAnimationHelper;
import com.whis.app.ui.components.ProtectionRing;

import java.util.ArrayList;
import java.util.List;

/**
 * Home dashboard fragment (UI_PLAN.md §2.4 / §3.1).
 * <p>
 * Features:
 * <ul>
 *   <li>ProtectionRing status card showing current protection health</li>
 *   <li>Contextual AI entry point card ("Ask Whis anything")</li>
 *   <li>Activity feed — starts empty on fresh install; populated by real AI-analysed data</li>
 *   <li>Empty state shown when no activity has been recorded yet</li>
 * </ul>
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. ProtectionRing status + breathing pulse animation
        ProtectionRing ring = view.findViewById(R.id.home_protection_ring);
        ring.setStatus(WhisVerdict.TRUSTED, 1.0f);
        startBreathingPulse(ring);

        // 2. "Ask Whis anything" card click handler → launches Whis AI Assistant
        view.findViewById(R.id.card_ask_whis_ai).setOnClickListener(v ->
                com.whis.app.agent.AgentLauncher.launch(requireContext()));

        // 3. Activity feed — populated by real AI-analysed detections
        RecyclerView rvFeed = view.findViewById(R.id.rv_activity_feed);
        rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<DetectionResult> feedItems = new ArrayList<>();
        List<com.whis.app.call.CallHistoryStore.CallEntry> storedCalls =
                com.whis.app.call.CallHistoryStore.getAll(requireContext());

        for (com.whis.app.call.CallHistoryStore.CallEntry call : storedCalls) {
            com.whis.app.core.WhisVerdict v;
            if ("SAFE".equals(call.riskLevel)) v = com.whis.app.core.WhisVerdict.TRUSTED;
            else if ("SCAM".equals(call.riskLevel)) v = com.whis.app.core.WhisVerdict.HIGH_RISK;
            else v = com.whis.app.core.WhisVerdict.SUSPICIOUS;

            com.whis.app.call.WhisCallAnalysis analysis = new com.whis.app.call.WhisCallAnalysis(
                    call.phoneNumber,
                    "SAFE".equals(call.riskLevel) ? 0 : 85,
                    v,
                    call.contactName + " (" + call.phoneNumber + ") - " + call.reason,
                    "SAVED_CONTACT".equals(call.category) ? "CONTACT" : "UNKNOWN_MOBILE",
                    "SAVED_CONTACT".equals(call.category),
                    false,
                    0
            );
            analysis.timestamp = call.timestamp;
            feedItems.add(analysis);
        }

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
                feedItems.add(res);
            }
        } catch (Exception e) {
            // Defensive
        }

        // Sort feed by timestamp DESC (newest activity first)
        java.util.Collections.sort(feedItems, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        final ActivityFeedAdapter[] adapterHolder = new ActivityFeedAdapter[1];
        ActivityFeedAdapter adapter = new ActivityFeedAdapter(feedItems, (item, position) -> {
            com.whis.app.ui.alert.AlertRenderer.showBottomSheetAlert(requireContext(), item, new com.whis.app.ui.alert.AlertRenderer.AlertActionListener() {
                @Override
                public void onPrimaryAction(String phoneNumber) {
                    if (item.getVerdict() == com.whis.app.core.WhisVerdict.HIGH_RISK) {
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            com.whis.app.call.BlockedNumberStore.block(requireContext(), phoneNumber);
                            android.widget.Toast.makeText(requireContext(),
                                    "🚫 Blocked: " + phoneNumber, android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.widget.Toast.makeText(requireContext(),
                                    "🚫 Blocked high-risk item", android.widget.Toast.LENGTH_SHORT).show();
                        }
                        feedItems.remove(position);
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
        rvFeed.setAdapter(adapter);

        // 4. Empty state — visible until real detections arrive
        View emptyState = view.findViewById(R.id.home_empty_state);
        View emptyIcon  = emptyState != null ? emptyState.findViewById(R.id.empty_state_icon) : null;
        if (feedItems.isEmpty() && emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
            if (emptyIcon != null) ListAnimationHelper.startIconFloat(emptyIcon);
        } else if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View root = getView();
        if (root != null) {
            View emptyState = root.findViewById(R.id.home_empty_state);
            if (emptyState != null) {
                View icon = emptyState.findViewById(R.id.empty_state_icon);
                if (icon != null) ListAnimationHelper.stopIconFloat(icon);
            }
        }
    }

    /**
     * Breathing pulse animation on the ProtectionRing.
     * Scale: 1.0 → 1.05 → 1.0, infinite loop, 2000ms, FastOutSlowInInterpolator.
     * Pivot is the view centre so the ring expands symmetrically.
     */
    private void startBreathingPulse(View target) {
        FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1.0f, 1.05f, 1.0f);
        scaleX.setDuration(2000);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleX.setInterpolator(interpolator);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1.0f, 1.05f, 1.0f);
        scaleY.setDuration(2000);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setInterpolator(interpolator);

        AnimatorSet pulse = new AnimatorSet();
        pulse.playTogether(scaleX, scaleY);
        pulse.start();

        target.setTag(R.id.home_protection_ring, pulse);
    }
}
