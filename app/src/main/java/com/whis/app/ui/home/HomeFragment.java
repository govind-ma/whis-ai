package com.whis.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.whis.app.R;
import com.whis.app.core.DetectionResult;
import com.whis.app.core.WhisVerdict;
import com.whis.app.ui.components.ListAnimationHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Home dashboard fragment (UI_PLAN.md §2.4 / §3.1).
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

        // 1. Top Bar 3-Dot Menu — all page actions
        View btnHomeMenu = view.findViewById(R.id.btn_home_menu);
        if (btnHomeMenu != null) {
            btnHomeMenu.setOnClickListener(v -> {
                androidx.appcompat.widget.PopupMenu popup =
                        new androidx.appcompat.widget.PopupMenu(requireContext(), v);
                popup.getMenu().add(0, 1, 0, "📄 Export Report");
                popup.getMenu().add(0, 2, 1, "🗑️ Clear Activity History");
                popup.getMenu().add(0, 3, 2, "☑️ Select Items");
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            // Export cybercrime report
                            exportReport(view);
                            return true;
                        case 2:
                            // Clear all call history — with confirmation
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Clear Activity History")
                                    .setMessage("Are you sure you want to delete all activity history? This cannot be undone.")
                                    .setPositiveButton("Clear", (dialog, which) -> {
                                        com.whis.app.call.CallHistoryStore.clear(requireContext());
                                        Toast.makeText(requireContext(), "Activity history cleared", Toast.LENGTH_SHORT).show();
                                        getParentFragmentManager().beginTransaction().detach(this).attach(this).commit();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            return true;
                        case 3:
                            // Show multi-select bar
                            View bar = view.findViewById(R.id.bar_multi_select);
                            if (bar != null) bar.setVisibility(View.VISIBLE);
                            return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

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

        // Deduplicate feed — prevents duplicate entries occurring within 10 seconds of each other
        List<DetectionResult> dedupedItems = new ArrayList<>();
        for (DetectionResult item : feedItems) {
            boolean isDuplicate = false;
            for (DetectionResult existing : dedupedItems) {
                boolean timeClose = Math.abs(item.getTimestamp() - existing.getTimestamp()) < 10000;
                String itemNum = com.whis.app.call.BlockedNumberStore.normalize(item.getReasonText());
                String existNum = com.whis.app.call.BlockedNumberStore.normalize(existing.getReasonText());
                boolean sameNum = !itemNum.isEmpty() && itemNum.equals(existNum);
                
                if (timeClose && (sameNum || item.getReasonText().contains("Unknown") || existing.getReasonText().contains("Unknown"))) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                dedupedItems.add(item);
            }
        }
        feedItems.clear();
        feedItems.addAll(dedupedItems);

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
                            Toast.makeText(requireContext(),
                                    "🚫 Blocked: " + phoneNumber, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(),
                                    "🚫 Blocked high-risk item", Toast.LENGTH_SHORT).show();
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

        // ── Filter Chips ──────────────────────────────────────────────────
        TextView chipAll = view.findViewById(R.id.chip_filter_all);
        TextView chipCalls = view.findViewById(R.id.chip_filter_calls);
        TextView chipMessages = view.findViewById(R.id.chip_filter_messages);
        TextView chipBlocked = view.findViewById(R.id.chip_filter_blocked);

        TextView[] chips = new TextView[]{chipAll, chipCalls, chipMessages, chipBlocked};

        if (chipAll != null && chipCalls != null && chipMessages != null && chipBlocked != null) {
            chipAll.setOnClickListener(v -> {
                updateChipSelection(chips, chipAll);
                filterFeed(feedItems, adapterHolder[0], "ALL");
            });
            chipCalls.setOnClickListener(v -> {
                updateChipSelection(chips, chipCalls);
                filterFeed(feedItems, adapterHolder[0], "CALLS");
            });
            chipMessages.setOnClickListener(v -> {
                updateChipSelection(chips, chipMessages);
                filterFeed(feedItems, adapterHolder[0], "MESSAGES");
            });
            chipBlocked.setOnClickListener(v -> {
                updateChipSelection(chips, chipBlocked);
                filterFeed(feedItems, adapterHolder[0], "BLOCKED");
            });
        }

        // ── Multi-Select Action Bar (Select All, Unselect All, Delete Selected) ──
        View barMultiSelect = view.findViewById(R.id.bar_multi_select);
        View btnSelectAll = view.findViewById(R.id.btn_select_all);
        View btnUnselectAll = view.findViewById(R.id.btn_unselect_all);
        android.widget.Button btnDeleteSelected = view.findViewById(R.id.btn_delete_selected);

        adapter.setOnSelectionChangeListener(count -> {
            if (barMultiSelect != null) {
                barMultiSelect.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
            }
            if (btnDeleteSelected != null) {
                btnDeleteSelected.setText(count > 0 ? "🗑️ Delete (" + count + ")" : "🗑️ Delete");
            }
            // Update block button label
            View btnBlockSel = view.findViewById(R.id.btn_block_selected);
            if (btnBlockSel instanceof android.widget.Button) {
                ((android.widget.Button) btnBlockSel).setText(count > 0 ? "🚫 Block (" + count + ")" : "🚫 Block");
            }
        });

        if (btnSelectAll != null && btnUnselectAll != null && btnDeleteSelected != null) {
            btnSelectAll.setOnClickListener(v -> {
                if (adapterHolder[0] != null) {
                    adapterHolder[0].selectAll();
                }
            });

            btnUnselectAll.setOnClickListener(v -> {
                if (adapterHolder[0] != null) {
                    adapterHolder[0].unselectAll();
                    if (barMultiSelect != null) barMultiSelect.setVisibility(View.GONE);
                }
            });

            btnDeleteSelected.setOnClickListener(v -> {
                if (adapterHolder[0] != null) {
                    List<DetectionResult> selected = adapterHolder[0].getSelectedItems();
                    if (selected.isEmpty()) {
                        Toast.makeText(requireContext(), "Select items to delete first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    java.util.Set<Long> timestampsToDelete = new java.util.HashSet<>();
                    for (DetectionResult item : selected) {
                        timestampsToDelete.add(item.getTimestamp());
                    }
                    com.whis.app.call.CallHistoryStore.deleteByTimestamps(requireContext(), timestampsToDelete);
                    feedItems.removeAll(selected);
                    adapterHolder[0].unselectAll();
                    adapterHolder[0].notifyDataSetChanged();
                    if (barMultiSelect != null) barMultiSelect.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Deleted " + selected.size() + " items", Toast.LENGTH_SHORT).show();
                }
            });

            // Block Selected — block all call numbers in selection
            android.widget.Button btnBlockSelected = view.findViewById(R.id.btn_block_selected);
            if (btnBlockSelected != null) {
                btnBlockSelected.setOnClickListener(v -> {
                    if (adapterHolder[0] != null) {
                        List<DetectionResult> selected = adapterHolder[0].getSelectedItems();
                        if (selected.isEmpty()) {
                            Toast.makeText(requireContext(), "Select items to block first", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int blockedCount = 0;
                        for (DetectionResult item : selected) {
                            if (item instanceof com.whis.app.call.WhisCallAnalysis) {
                                String num = ((com.whis.app.call.WhisCallAnalysis) item).incomingNumber;
                                if (num != null && !num.isEmpty()) {
                                    com.whis.app.call.BlockedNumberStore.block(requireContext(), num);
                                    blockedCount++;
                                }
                            }
                        }
                        adapterHolder[0].unselectAll();
                        adapterHolder[0].notifyDataSetChanged();
                        if (barMultiSelect != null) barMultiSelect.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "🚫 Blocked " + blockedCount + " number(s)", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

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

    private void updateChipSelection(TextView[] chips, TextView activeChip) {
        for (TextView chip : chips) {
            if (chip == null) continue;
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_active);
                chip.setTextColor(getResources().getColor(R.color.whis_bg, requireContext().getTheme()));
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive);
                chip.setTextColor(getResources().getColor(R.color.whis_text_hi, requireContext().getTheme()));
            }
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



    private void filterFeed(List<DetectionResult> allItems, ActivityFeedAdapter adapter, String filter) {
        List<DetectionResult> filtered = new ArrayList<>();
        for (DetectionResult item : allItems) {
            if ("ALL".equals(filter)) {
                filtered.add(item);
            } else if ("CALLS".equals(filter) && "CALL".equals(item.getSourceType())) {
                // Only show items from the call module
                filtered.add(item);
            } else if ("MESSAGES".equals(filter) && "SMS".equals(item.getSourceType())) {
                // Only show items from the SMS/msg module
                filtered.add(item);
            } else if ("BLOCKED".equals(filter)) {
                // Show items whose number is in the blocked list
                String num = null;
                if (item instanceof com.whis.app.call.WhisCallAnalysis) {
                    num = ((com.whis.app.call.WhisCallAnalysis) item).incomingNumber;
                }
                if (num != null && com.whis.app.call.BlockedNumberStore.isBlocked(requireContext(), num)) {
                    filtered.add(item);
                }
            }
        }
        ActivityFeedAdapter newAdapter = new ActivityFeedAdapter(filtered, (item, position) -> {
            com.whis.app.ui.alert.AlertRenderer.showBottomSheetAlert(requireContext(), item, null);
        });
        RecyclerView rvFeed = getView() != null ? getView().findViewById(R.id.rv_activity_feed) : null;
        if (rvFeed != null) rvFeed.setAdapter(newAdapter);
    }

    private void exportReport(View view) {
        // Collect all current feed items and export
        List<DetectionResult> items = new ArrayList<>();
        try {
            List<com.whis.app.call.CallHistoryStore.CallEntry> calls =
                    com.whis.app.call.CallHistoryStore.getAll(requireContext());
            for (com.whis.app.call.CallHistoryStore.CallEntry call : calls) {
                com.whis.app.core.WhisVerdict v;
                if ("SAFE".equals(call.riskLevel)) v = WhisVerdict.TRUSTED;
                else if ("SCAM".equals(call.riskLevel)) v = WhisVerdict.HIGH_RISK;
                else v = WhisVerdict.SUSPICIOUS;
                com.whis.app.call.WhisCallAnalysis a = new com.whis.app.call.WhisCallAnalysis(
                        call.phoneNumber, "SAFE".equals(call.riskLevel) ? 0 : 85, v,
                        call.contactName + " (" + call.phoneNumber + ") - " + call.reason,
                        "SAVED_CONTACT".equals(call.category) ? "CONTACT" : "UNKNOWN_MOBILE",
                        "SAVED_CONTACT".equals(call.category), false, 0);
                a.timestamp = call.timestamp;
                items.add(a);
            }
        } catch (Exception ignored) {}
        exportCybercrimeReport(items);
    }

    private void exportCybercrimeReport(List<DetectionResult> items) {
        StringBuilder report = new StringBuilder();
        report.append("========================================\n");
        report.append("  WHIS AI — CYBERCRIME THREAT REPORT\n");
        report.append("  Generated: ").append(new Date()).append("\n");
        report.append("========================================\n\n");

        int count = 0;
        for (DetectionResult item : items) {
            if (item.getVerdict() == WhisVerdict.HIGH_RISK || item.getVerdict() == WhisVerdict.SUSPICIOUS) {
                count++;
                report.append("INCIDENT #").append(count).append("\n");
                report.append("• Date/Time: ").append(new Date(item.getTimestamp())).append("\n");
                report.append("• Risk Level: ").append(item.getVerdict().name()).append("\n");
                report.append("• Category: ").append(item.getIdentifierType()).append("\n");
                report.append("• Details: ").append(item.getReasonText() != null ? item.getReasonText() : "N/A").append("\n");
                report.append("----------------------------------------\n");
            }
        }

        if (count == 0) {
            report.append("No active cyber fraud threats recorded.\n");
        }

        report.append("\nReporting Channels:\n");
        report.append("• National Helpline: 1930\n");
        report.append("• Cybercrime Portal: cybercrime.gov.in\n");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "WHIS AI Cybercrime Threat Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, report.toString());
        startActivity(Intent.createChooser(shareIntent, "Share Cybercrime Report"));
    }
}
