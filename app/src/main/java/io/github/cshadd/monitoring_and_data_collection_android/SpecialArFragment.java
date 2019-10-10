package io.github.cshadd.monitoring_and_data_collection_android;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;

public class SpecialArFragment extends ArFragment {

    public SpecialArFragment() {
        super();
        return;
    }

    @Override
    protected Config getSessionConfiguration(Session session) {
        final Config config = super.getSessionConfiguration(session);
        config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
        return config;
    }
}