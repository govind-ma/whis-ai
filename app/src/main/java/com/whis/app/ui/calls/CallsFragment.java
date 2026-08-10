package com.whis.app.ui.calls;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
 * CallsFragment — Call Protection module UI (UI_PLAN.md §2.4 / §3.5).
 * <p>
 * Displays AI-analysed incoming calls screened since protection was turned on.
 * Starts empty on fresh install; the {@link com.whis.app.call.CallFilterService}
 * populates data as real calls arrive and are analysed by Gemini.
 * <p>
 * Animation layers:
 * <ul>
 *   <li>Staggered slide-up entrance on first RecyclerView load (cap=5, 60ms stagger)</li>
 *   <li>Swipe left = "Mark Safe" (green), swipe right = "Report Scam" (red) via
 *       {@link WhisSwipeActionCallback}; partial swipes spring back</li>
 *   <li>Empty state with floating shield icon when list is empty</li>
 * </ul>
 */
public class CallsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calls, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Top Bar 3-Dot Overflow Menu Handler
        View btnMenu = view.findViewById(R.id.btn_calls_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), v);
                popup.getMenu().add("🗑️ Clear Call History");
                popup.getMenu().add("🚫 Clear All Blocked Numbers");
                popup.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle().toString();
                    if (title.contains("Clear Call History")) {
                        com.whis.app.call.CallHistoryStore.clear(requireContext());
                        Toast.makeText(requireContext(), "Call history cleared", Toast.LENGTH_SHORT).show();
                        // Refresh fragment view
                        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                        return true;
                    } else if (title.contains("Clear All Blocked Numbers")) {
                        com.whis.app.call.BlockedNumberStore.clearAll(requireContext());
                        Toast.makeText(requireContext(), "All blocked numbers cleared", Toast.LENGTH_SHORT).show();
                        LinearLayout containerBlocked = view.findViewById(R.id.container_blocked_numbers);
                        if (containerBlocked != null) renderBlockedNumbers(containerBlocked);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // 2. Blocked Numbers Management section
        LinearLayout containerBlocked = view.findViewById(R.id.container_blocked_numbers);
        renderBlockedNumbers(containerBlocked);

        // 2. Feed starts empty — populated by real Gemini-analysed calls from CallFilterService
        List<DetectionResult> callItems = new ArrayList<>();
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
            callItems.add(analysis);
        }

        // 3. Empty state
        View emptyState = view.findViewById(R.id.calls_empty_state);
        View emptyIcon  = emptyState.findViewById(R.id.empty_state_icon);
        if (callItems.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            ListAnimationHelper.startIconFloat(emptyIcon);
        } else {
            emptyState.setVisibility(View.GONE);
        }

        // 4. Screened Calls RecyclerView
        RecyclerView rvFeed = view.findViewById(R.id.rv_calls_feed);
        rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));

        final ActivityFeedAdapter[] adapterHolder = new ActivityFeedAdapter[1];
        ActivityFeedAdapter adapter = new ActivityFeedAdapter(callItems, (item, position) -> {
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
                                    "🚫 Blocked high-risk call", Toast.LENGTH_SHORT).show();
                        }
                        callItems.remove(position);
                        if (adapterHolder[0] != null) adapterHolder[0].notifyItemRemoved(position);
                        // Refresh blocked panel
                        renderBlockedNumbers(containerBlocked);
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
                if (adapterPosition < callItems.size()) {
                    DetectionResult item = callItems.get(adapterPosition);
                    // Remove from persistent storage
                    java.util.Set<Long> ts = new java.util.HashSet<>();
                    ts.add(item.getTimestamp());
                    com.whis.app.call.CallHistoryStore.deleteByTimestamps(requireContext(), ts);

                    Toast.makeText(requireContext(), "✅ Marked safe", Toast.LENGTH_SHORT).show();
                    callItems.remove(adapterPosition);
                    adapter.notifyItemRemoved(adapterPosition);
                    if (callItems.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        ListAnimationHelper.startIconFloat(emptyIcon);
                    }
                }
            }

            @Override
            public void onReportScam(int adapterPosition) {
                if (adapterPosition < callItems.size()) {
                    DetectionResult item = callItems.get(adapterPosition);
                    String phone = null;
                    if (item instanceof com.whis.app.call.WhisCallAnalysis) {
                        phone = ((com.whis.app.call.WhisCallAnalysis) item).incomingNumber;
                    }

                    // 1. Block the number persistently
                    if (phone != null && !phone.isEmpty()) {
                        com.whis.app.call.BlockedNumberStore.block(requireContext(), phone);
                        Toast.makeText(requireContext(), "🚫 Reported & Blocked: " + phone, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "🚫 Reported scam call", Toast.LENGTH_SHORT).show();
                    }

                    // 2. Remove from persistent call history storage
                    java.util.Set<Long> ts = new java.util.HashSet<>();
                    ts.add(item.getTimestamp());
                    com.whis.app.call.CallHistoryStore.deleteByTimestamps(requireContext(), ts);

                    // 3. Update list UI
                    callItems.remove(adapterPosition);
                    adapter.notifyItemRemoved(adapterPosition);
                    if (callItems.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        ListAnimationHelper.startIconFloat(emptyIcon);
                    }

                    // 4. Refresh blocked numbers UI section above
                    renderBlockedNumbers(containerBlocked);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View root = getView();
        if (root != null) {
            View emptyState = root.findViewById(R.id.calls_empty_state);
            if (emptyState != null) {
                View icon = emptyState.findViewById(R.id.empty_state_icon);
                if (icon != null) ListAnimationHelper.stopIconFloat(icon);
            }
        }
    }

    private void renderBlockedNumbers(LinearLayout container) {
        container.removeAllViews();
        java.util.List<String> blockedNumbers =
                com.whis.app.call.BlockedNumberStore.getAll(requireContext());

        if (blockedNumbers.isEmpty()) {
            com.whis.app.ui.components.WhisListRow emptyRow =
                    new com.whis.app.ui.components.WhisListRow(requireContext());
            emptyRow.hideIcon();
            emptyRow.setTitle("No blocked numbers");
            emptyRow.setSubtitle("Numbers you block will appear here");
            container.addView(emptyRow);
            return;
        }

        for (String blocked : blockedNumbers) {
            com.whis.app.ui.components.WhisListRow row =
                    new com.whis.app.ui.components.WhisListRow(requireContext());
            row.setIcon(R.drawable.ic_nav_calls);
            row.setTitle(blocked);
            row.setSubtitle("Blocked — calls will be rejected silently");
            row.setVerdict(com.whis.app.core.WhisVerdict.HIGH_RISK);

            android.widget.Button btnUnblock = new android.widget.Button(requireContext());
            btnUnblock.setText("Unblock");
            btnUnblock.setTextSize(13);
            btnUnblock.setAllCaps(false);
            btnUnblock.setOnClickListener(v -> {
                com.whis.app.call.BlockedNumberStore.unblock(requireContext(), blocked);
                Toast.makeText(requireContext(), "✅ Unblocked " + blocked, Toast.LENGTH_SHORT).show();
                renderBlockedNumbers(container);
            });

            row.addView(btnUnblock);
            container.addView(row);
        }
    }
}
