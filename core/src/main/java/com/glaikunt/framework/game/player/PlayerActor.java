package com.glaikunt.framework.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.glaikunt.framework.application.ApplicationResources;
import com.glaikunt.framework.application.CommonActor;

import com.glaikunt.framework.cache.MusicCache;
import com.glaikunt.framework.cache.TextureCache;
import com.glaikunt.framework.esc.component.animation.AnimationComponent;
import com.glaikunt.framework.esc.component.common.*;
import com.glaikunt.framework.esc.component.movement.AbstractPlayerInputComponent;
import com.glaikunt.framework.esc.component.movement.PlayerInputComponent;
import com.glaikunt.framework.esc.system.physics.BodyComponent;
import com.glaikunt.framework.esc.system.physics.BodyType;

import java.util.List;
import java.util.stream.Collectors;

public class PlayerActor extends CommonActor {

    private final AccelerationComponent acceleration;
    private final VelocityComponent velocity;
    private final AnimationComponent idleAnimation, runningAnimation;
    private final PlayerInputComponent playerInput;

    private final WarmthComponent warmth;
    private final BodyComponent body;

    private final Vector2 tmpVector2 = new Vector2();
    private static final float AUDIO_RAMP = 5f;

    public PlayerActor(ApplicationResources applicationResources, Vector2 pos) {
        super(applicationResources);

        this.acceleration = new AccelerationComponent();
        this.velocity = new VelocityComponent();

        this.idleAnimation = new AnimationComponent(applicationResources.getCacheRetriever().geTextureCache(TextureCache.IDLE_PLAYER), 6, 1);
        this.idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
        this.idleAnimation.setFramerate(0.15f);

        this.runningAnimation = new AnimationComponent(applicationResources.getCacheRetriever().geTextureCache(TextureCache.RUNNING_PLAYER), 4, 1);
        this.runningAnimation.setPlayMode(Animation.PlayMode.LOOP);
        this.runningAnimation.setFramerate(0.1f);

        this.playerInput = new PlayerInputComponent();
        this.warmth = new WarmthComponent(WarmthComponent.WARMTH_MAX);

        this.pos.set(pos);
        this.size.set(idleAnimation.getCurrentFrame().getRegionWidth(), idleAnimation.getCurrentFrame().getRegionHeight());

        this.body = new BodyComponent();
        this.body.setBodyType(BodyType.PLAYER);
        this.body.set(getX()+1, getY()+1, getWidth()-2, getHeight()-2);

        getEntity().add(acceleration);
        getEntity().add(velocity);
        getEntity().add(idleAnimation);
        getEntity().add(playerInput);
        getEntity().add(warmth);
        getEntity().add(body);
        getEntity().add(getApplicationResources().getGlobalEntity().getComponent(GravityComponent.class));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

        batch.setColor(warmth.getWarmthFloat(), warmth.getWarmthFloat(), 1.0f, 1f);
        batch.draw(getEntity().getComponent(AnimationComponent.class).getCurrentFrame(), getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);
    }

    @Override
    public void act(float delta) {

        if (playerInput.getFacing().equals(AbstractPlayerInputComponent.Direction.RIGHT) && getEntity().getComponent(AnimationComponent.class).isxFlip()) {
            getEntity().getComponent(AnimationComponent.class).setxFlip(false);
            for (TextureRegion region : getEntity().getComponent(AnimationComponent.class).getCurrentAnimation().getKeyFrames()) {
                region.flip(true, false);
            }
        } else  if (playerInput.getFacing().equals(AbstractPlayerInputComponent.Direction.LEFT) && !getEntity().getComponent(AnimationComponent.class).isxFlip()) {
            getEntity().getComponent(AnimationComponent.class).setxFlip(true);
            for (TextureRegion region : getEntity().getComponent(AnimationComponent.class).getCurrentAnimation().getKeyFrames()) {
                region.flip(true, false);
            }
        }

        if (playerInput.getAnimation().equals(AbstractPlayerInputComponent.Animation.IDLE) && !getEntity().getComponents().contains(idleAnimation, true)) {
            getEntity().add(idleAnimation);
        } else if (playerInput.getAnimation().equals(AbstractPlayerInputComponent.Animation.MOVEMENT) && !getEntity().getComponents().contains(runningAnimation, true)) {
            getEntity().add(runningAnimation);
        }

        cameraUpdate(delta);

        if (!getBody().getBeforeContacts().isEmpty()) {
            Gdx.app.log("DEBUG", "[P] Before Collide Intersection: " + getBody().getBeforeContacts().size() + ", and body contacts is now: " + getBody().getContactsByBody().size());
            List<Vector2> collect = getBody().getBeforeContacts().stream().map(ContactComponent::getNormal)
                    .collect(Collectors.toList());
            Gdx.app.log("DEBUG", "[P] Before Collide normal mappings: " + collect);
        }
        if (!getBody().getAfterContacts().isEmpty()) {
            Gdx.app.log("DEBUG", "[P] After Collide Intersection: " + getBody().getAfterContacts().size() + ", and body contacts is now: " + getBody().getContactsByBody().size());
        }

        if (warmth.isOutside()) {
            if (!getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).isPlaying()) {
                getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).setLooping(true);
                getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).play();
            }
            if (getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).getVolume() < 1f) {
                getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).setVolume(Math.min(1f, getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).getVolume()+(delta*AUDIO_RAMP)));
            }
            if (getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).getVolume() > 0f) {
                getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).setVolume(Math.max(0f, getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).getVolume()-(delta*AUDIO_RAMP)));
            }
        } else {
            if (!getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).isPlaying()) {
                getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).setLooping(true);
                getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).play();
            }
            if (getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).getVolume() < 1f) {
                getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).setVolume(Math.min(1f, getApplicationResources().getMusic(MusicCache.BLIZZARD_INTERNAL).getVolume()+(delta*AUDIO_RAMP)));
            }
            if (getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).getVolume() > 0f) {
                getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).setVolume(Math.max(0f, getApplicationResources().getMusic(MusicCache.BLIZZARD_EXTERNAL).getVolume()-(delta*AUDIO_RAMP)));
            }
        }
    }

    private void cameraUpdate(float delta) {
        getStage().getCamera().position.x = MathUtils.lerp(getStage().getCamera().position.x, getX() + (getWidth() / 2), 5 * delta);
        getStage().getCamera().position.y = MathUtils.lerp(getStage().getCamera().position.y, (getY()) + (getHeight() * 3), 5 * delta);

//        float newZoom = GameConstants.ZOOM
//                + tmpVector2.set(getStage().getCamera().position.x, getStage().getCamera().position.y)
//                .sub(getX() + (getWidth() / 2), getY() + (getHeight()*3)).scl(.001f)
//                .len();
//        ((OrthographicCamera) getStage().getCamera()).zoom = MathUtils.lerp(((OrthographicCamera) getStage().getCamera()).zoom, newZoom, 1 * Gdx.graphics.getDeltaTime());
    }

    @Override
    public void drawDebug(ShapeRenderer shapes) {

        shapes.rect(getBodyRect().getX(), getBodyRect().getY(), getBodyRect().width, getBodyRect().height);
    }

    public BodyComponent getBody() {
        return body;
    }

    public Rectangle getBodyRect() {
        return body;
    }

    public PlayerInputComponent getPlayerInput() {
        return playerInput;
    }
}
