package com.whis.app.ui.alert;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.whis.app.R;
import com.whis.app.ui.components.PrimaryButton;
import com.whis.app.ui.components.RiskTag;

/**
 * Generic detail screen for screened call/message alerts (UI_PLAN.md §2.4 / §3.1).
 * <p>
 * Currently shows a placeholder when no real detection data has been passed yet.
 * Will be fully wired to real {@link com.whis.app.core.DetectionResult} objects
 * when the Calls/Messages feed connects item-tap navigation to this screen.
 */
public class AlertDetailFragment extends Fragment {

    public static final String ARG_SAMPLE_INDEX = "sample_index";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alert_detail, container, false);
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        RiskTag      riskTag     = view.findViewById(R.id.detail_risk_tag);
        TextView     tvTimestamp = view.findViewById(R.id.detail_timestamp);
        TextView     tvTitle     = view.findViewById(R.id.detail_title);
        TextView     tvCopy      = view.findViewById(R.id.detail_formatted_copy);
        TextView     tvScore     = view.findViewById(R.id.detail_risk_score);
        TextView     tvIdType    = view.findViewById(R.id.detail_identifier_type);
        TextView     tvSource    = view.findViewById(R.id.detail_confidence_source);
        PrimaryButton btnAskAi   = view.findViewById(R.id.btn_detail_ask_ai);
        Button        btnBack    = view.findViewById(R.id.btn_detail_back);

        // Placeholder — real data will be passed via Bundle arguments
        // once CallsFragment / MessagesFragment connect real DetectionResult objects
        tvTitle.setText("Alert Detail");
        tvTimestamp.setText("Just now");
        tvCopy.setText("No detail data available yet. AI-analysed results will appear here after your first screened call or SMS.");
        tvScore.setText("Risk Score: —");
        tvIdType.setText("Identifier Type: —");
        tvSource.setText("Confidence Source: —");

        if (btnAskAi != null) {
            btnAskAi.setOnClickListener(v ->
                    com.whis.app.agent.AgentLauncher.launch(requireContext()));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    Navigation.findNavController(v).navigateUp());
        }
    }
}
