package com.glaikunt.framework.game.map.levels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.glaikunt.framework.FrameworkConstants;
import com.glaikunt.framework.application.ApplicationResources;
import com.glaikunt.framework.application.CommonActor;
import com.glaikunt.framework.application.TickTimer;
import com.glaikunt.framework.cache.TextureCache;
import com.glaikunt.framework.credits.CreditScreen;
import com.glaikunt.framework.effects.FogActor;
import com.glaikunt.framework.esc.component.misc.FadeComponent;
import com.glaikunt.framework.esc.system.physics.BodyComponent;
import com.glaikunt.framework.esc.system.physics.BodyType;
import com.glaikunt.framework.game.GameConstants;
import com.glaikunt.framework.game.player.PlayerActor;
import com.glaikunt.framework.pixels.PixelBlizzardActor;
import com.glaikunt.framework.pixels.PixelStarsActor;

import java.util.ArrayList;
import java.util.List;

public class LevelController extends CommonActor {

    private final Stage front;
    private final Stage background;
    private final List<AbstractLevel> levels = new ArrayList<>();
    private AbstractLevel currentLevel;
    private PlayerActor currentPlayer;
    private final Texture pixel;
    private PixelBlizzardActor blizzard;
    private FogActor fogActor;
    private FogActor fogActor2;

    private final FadeComponent.Fade fade;

    private boolean startLevelTransition;
    private boolean resetLevel;
    private final TickTimer resetLevelTimer = new TickTimer(2f);

    public LevelController(ApplicationResources applicationResources, Stage front, Stage background) {
        super(applicationResources);

        this.front = front;
        this.background = background;
        this.pixel = applicationResources.getTexture(TextureCache.PIXEL);

        this.levels.add(new Level1(applicationResources, front));
        this.levels.add(new Level2(applicationResources, front));
        this.levels.add(new Level3(applicationResources, front));
        this.levels.add(new Level4(applicationResources, front));
        this.levels.add(new Level5(applicationResources, front));
        this.levels.add(new Level6(applicationResources, front));

//        this.levels.add(new DebugLevel(applicationResources, front));
//        this.levels.add(new NextDebugLevel(applicationResources, front));

        this.currentLevel = levels.get(0);
        this.currentLevel.init();
        this.currentPlayer = currentLevel.getPlayer();
        this.levels.remove(0);

        createEffects(front, background);

        FadeComponent fadeComponent = new FadeComponent();
        this.fade = new FadeComponent.Fade();
        this.fade.setFade(0);
//        this.fade.setSpeed(1);
        this.fade.setMaxFade(1);
        fadeComponent.addFade(fade);
        getEntity().add(fadeComponent);
    }

    private void createEffects(Stage front, Stage background) {
        background.addActor(new PixelStarsActor(getApplicationResources(), FrameworkConstants.WHITE));
        front.addActor(blizzard = new PixelBlizzardActor(getApplicationResources(), FrameworkConstants.WHITE));
        front.addActor(fogActor = new FogActor(getApplicationResources(), 0.04f));
        front.addActor(fogActor2 = new FogActor(getApplicationResources(), 0.011f));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

        if (fade.isFadeOut() || fade.isFadeIn()) {
            batch.setColor(0, 0, 0, fade.getFade());
            batch.draw(pixel, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setColor(1, 1, 1, 1);
        }
    }

    @Override
    public void act(float delta) {

        updateEffects();

        levelTransitionUpdate();

        resetLevelUpdate(delta);

        if (fade.getFade() == 0 && fade.isFadeOut()) {
            startLevelTransition = false;
            resetLevel = false;
            currentPlayer.getPlayerInput().setDisableInputMovement(false);
            fade.setFadeOut(false);
        }
    }

    private void updateEffects() {
        front.getCamera().update();
        getBlizzard().updatePosition(front.getCamera().position.x, front.getCamera().position.y);
        getFogActor().updatePosition(front.getCamera().position.x, front.getCamera().position.y);
        getFogActor2().updatePosition(front.getCamera().position.x, front.getCamera().position.y);
    }

    private void resetLevelUpdate(float delta) {
        if (!resetLevel && !startLevelTransition) {

            if (getPlayer().getPlayer().isDead()) {
                resetLevel = true;
            }
        }

        if (resetLevel && !fade.isFadeOut() && !fade.isFadeIn()) {
            resetLevelTimer.tick(delta);
            if (resetLevelTimer.isTimerEventReady()) {
                fade.setFadeIn(true);
            }
        }

        if (fade.getFade() >= 1 && resetLevel) {
            front.clear();
            getEngine().removeAllEntities();
            getEngine().addEntity(getEntity());
            getCurrentLevel().reset();
            getCurrentLevel().init();
            this.currentPlayer = currentLevel.getPlayer();
            ((OrthographicCamera) front.getCamera()).zoom = GameConstants.ZOOM;
            front.getCamera().position.set(getPlayer().getX() + (getPlayer().getWidth() / 2), (getPlayer().getY()) + (getPlayer().getHeight()*2), 0);
            createEffects(front, background);
            fade.setFadeOut(true);
        }
    }

    private void levelTransitionUpdate() {
        if (!startLevelTransition && !resetLevel) {

            for (BodyComponent contract : currentPlayer.getBody().getContactsByBody().keySet()) {

                if (contract.getBodyType().equals(BodyType.CHECKPOINT)) {

                    if (levels.isEmpty()) {
                        getApplicationResources().getDisplay().setScreen(new CreditScreen(getApplicationResources()));
                    } else {
                        startLevelTransition = true;
                        fade.setFadeIn(true);
                        currentPlayer.getPlayerInput().setDisableInputMovement(true);
                        currentPlayer.getPlayerInput().setWalkRight(true);
                        currentPlayer.getPlayer().setLevelComplete(true);
                    }
                }
            }

        }

        if (fade.getFade() >= 1 && startLevelTransition) {
            front.clear();
            getEngine().removeAllEntities();
            getEngine().addEntity(getEntity());

            currentLevel = levels.get(0);
            currentLevel.init();
            currentPlayer = currentLevel.getPlayer();
            levels.remove(0);
            ((OrthographicCamera) front.getCamera()).zoom = GameConstants.ZOOM;
            front.getCamera().position.set(getPlayer().getX() + (getPlayer().getWidth() / 2), (getPlayer().getY()) + (getPlayer().getHeight()*2), 0);
            createEffects(front, background);
            fade.setFadeOut(true);
            currentPlayer.getPlayerInput().setDisableInputMovement(true);
        }
    }

    public AbstractLevel getCurrentLevel() {
        return currentLevel;
    }

    public PlayerActor getPlayer() {
        return currentPlayer;
    }

    public PixelBlizzardActor getBlizzard() {
        return blizzard;
    }

    public FogActor getFogActor() {
        return fogActor;
    }

    public FogActor getFogActor2() {
        return fogActor2;
    }
}
