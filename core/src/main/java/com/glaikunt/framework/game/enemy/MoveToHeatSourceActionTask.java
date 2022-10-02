package com.glaikunt.framework.game.enemy;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.glaikunt.framework.Ansi;

public class MoveToHeatSourceActionTask extends AbstractLeafTask {
    public MoveToHeatSourceActionTask(Entity entity) {
        super(entity);
    }

    @Override
    public Status execute() {
        System.out.println( Ansi.red("[AI] ")+Ansi.yellow("execute MoveToHeatSourceActionTask"));
        System.out.println( Ansi.red("  |- ")+Ansi.green("Status.SUCCEEDED"));
        return Status.SUCCEEDED;
    }
}