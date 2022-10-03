package com.glaikunt.framework.game.enemy;

import com.badlogic.ashley.core.Entity;
import com.glaikunt.framework.Ansi;
import com.glaikunt.framework.application.ApplicationResources;
import com.glaikunt.framework.esc.component.common.WarmthComponent;
import com.glaikunt.framework.game.GameConstants;

public class HeatUpConditionTask extends AbstractLeafTask {
    private final WarmthComponent warmth;
    public HeatUpConditionTask(Entity entity, ApplicationResources applicationResources) {
        super(entity, applicationResources);
        this.warmth = entity.getComponent(WarmthComponent.class);
    }

    @Override
    public Status execute() {
        if (GameConstants.BEHAVIOUR_LOGGING) System.out.println( Ansi.red("[AI] ")+Ansi.yellow("execute TooColdConditionTask [")+Ansi.green(warmth.toString())+Ansi.yellow("]"));
        if (warmth.isFrozen() || warmth.getWarmthFloat() < 0.8f) {
            if (GameConstants.BEHAVIOUR_LOGGING) System.out.println( Ansi.red("  |- ")+Ansi.cyan("** CHILLY **"));
            if (GameConstants.BEHAVIOUR_LOGGING) System.out.println( Ansi.red("  |- ")+Ansi.green("Status.SUCCEEDED"));
            return Status.SUCCEEDED;
        } else {
            if (GameConstants.BEHAVIOUR_LOGGING) System.out.println( Ansi.red("  |- ")+Ansi.red("Status.FAILED"));
            return Status.FAILED;
        }
    }
}
