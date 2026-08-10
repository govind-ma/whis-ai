package com.whis.app.ui.calls;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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
                androidx.appcompat.widget.PopupMenu popup =
                        new androidx.appcompat.widget.PopupMenu(requireContext(), v);
                popup.getMenu().add(0, 1, 0, "🚫 View Blocked Numbers");
                popup.getMenu().add(0, 2, 1, "🗑️ Clear Call History");
                popup.getMenu().add(0, 3, 2, "🧹 Clear All Blocked Numbers");
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == 1) {
                        showBlockedNumbersDialog();
                        return true;
                    } else if (id == 2) {
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Clear Call History")
                                .setMessage("Are you sure you want to delete all screened call history? This cannot be undone.")
                                .setPositiveButton("Clear", (dialog, which) -> {
                                    com.whis.app.call.CallHistoryStore.clear(requireContext());
                                    Toast.makeText(requireContext(), "Call history cleared", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    } else if (id == 3) {
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Clear All Blocked Numbers")
                                .setMessage("Are you sure you want to unblock all numbers? They will no longer be blocked.")
                                .setPositiveButton("Clear All", (dialog, which) -> {
                                    com.whis.app.call.BlockedNumberStore.clearAll(requireContext());
                                    Toast.makeText(requireContext(), "All blocked numbers cleared", Toast.LENGTH_SHORT).show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

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
                            // Keep item in list — just refresh the row to show "🚫 Blocked" chip
                            if (adapterHolder[0] != null) adapterHolder[0].notifyItemChanged(position);
                            Toast.makeText(requireContext(),
                                    "🚫 Blocked: " + phoneNumber + ". Future calls will be rejected.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "🚫 Blocked high-risk call", Toast.LENGTH_SHORT).show();
                        }
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
                        Toast.makeText(requireContext(),
                                "🚫 Reported & Blocked: " + phone + ". Future calls will be rejected.",
                                Toast.LENGTH_LONG).show();
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

    // ── Blocked Numbers Dialog ────────────────────────────────────────────────

    /**
     * Shows a bottom-sheet-style dialog listing all blocked numbers with Unblock actions.
     */
    private void showBlockedNumbersDialog() {
        if (!isAdded() || getContext() == null) return;

        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        // Build content dynamically
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        root.setPadding(pad, pad, pad, pad);

        // Title row
        TextView title = new TextView(requireContext());
        title.setText("🚫 Blocked Numbers");
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, dpToPx(12));
        root.addView(title);

        // Blocked list
        List<String> blockedNumbers =
                com.whis.app.call.BlockedNumberStore.getAll(requireContext());

        if (blockedNumbers.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No blocked numbers. Numbers you block will appear here.");
            empty.setTextSize(14f);
            root.addView(empty);
        } else {
            for (String blocked : blockedNumbers) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, dpToPx(8), 0, dpToPx(8));

                TextView numText = new TextView(requireContext());
                numText.setText(blocked);
                numText.setTextSize(15f);
                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                numText.setLayoutParams(lp);
                row.addView(numText);

                android.widget.Button btnUnblock = new android.widget.Button(requireContext());
                btnUnblock.setText("Unblock");
                btnUnblock.setTextSize(12f);
                btnUnblock.setAllCaps(false);
                final String finalBlocked = blocked;
                btnUnblock.setOnClickListener(v -> {
                    com.whis.app.call.BlockedNumberStore.unblock(requireContext(), finalBlocked);
                    Toast.makeText(requireContext(), "✅ Unblocked " + finalBlocked, Toast.LENGTH_SHORT).show();
                    row.setVisibility(View.GONE);
                });
                row.addView(btnUnblock);
                root.addView(row);

                // Divider
                View divider = new View(requireContext());
                LinearLayout.LayoutParams divLp =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(0xFFEEEEEE);
                root.addView(divider);
            }
        }

        // Close button
        android.widget.Button btnClose = new android.widget.Button(requireContext());
        btnClose.setText("Close");
        btnClose.setAllCaps(false);
        LinearLayout.LayoutParams closeLp =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        closeLp.gravity = android.view.Gravity.END;
        closeLp.topMargin = dpToPx(12);
        btnClose.setLayoutParams(closeLp);
        btnClose.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnClose);

        scrollView.addView(root);
        dialog.setContentView(scrollView);
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
