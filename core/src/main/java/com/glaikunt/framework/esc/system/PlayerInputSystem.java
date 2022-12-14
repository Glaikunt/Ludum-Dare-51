package com.glaikunt.framework.esc.system;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.glaikunt.framework.application.GameUtils;
import com.glaikunt.framework.esc.component.animation.AnimationComponent;
import com.glaikunt.framework.esc.component.common.AccelerationComponent;
import com.glaikunt.framework.esc.component.common.VelocityComponent;
import com.glaikunt.framework.esc.component.common.WarmthComponent;
import com.glaikunt.framework.esc.component.movement.AbstractPlayerInputComponent;
import com.glaikunt.framework.esc.component.movement.PlayerInputComponent;
import com.glaikunt.framework.esc.system.physics.BodyComponent;

public class PlayerInputSystem extends EntitySystem {
    private static final float LATERAL_ACCELERATION = 50f;
    private static final float JUMPING_ACCELERATION = 75;
    private final ImmutableArray<Entity> animationEntities;
    private final ComponentMapper<AccelerationComponent> acm = ComponentMapper.getFor(AccelerationComponent.class);
    private final ComponentMapper<PlayerInputComponent> pic = ComponentMapper.getFor(PlayerInputComponent.class);
    private final ComponentMapper<BodyComponent> bcm = ComponentMapper.getFor(BodyComponent.class);
    private final ComponentMapper<WarmthComponent> wcm = ComponentMapper.getFor(WarmthComponent.class);

    //TODO Jumping State

    public PlayerInputSystem(Engine engine) {
        animationEntities = engine.getEntitiesFor(
                Family
                        .one(PlayerInputComponent.class)
                        .all(AnimationComponent.class, VelocityComponent.class, BodyComponent.class).get()
        );
    }

    @Override
    public void update(float delta) {

        for (int i = 0; i < animationEntities.size(); ++i) {

            Entity entity = animationEntities.get(i);
            AccelerationComponent ac = acm.get(entity);
            AbstractPlayerInputComponent input = pic.get(entity);
            BodyComponent body = bcm.get(entity);
            WarmthComponent warmth = wcm.get(entity);

            if (!input.isDisableInputMovement()) {

                if (input.isMovingLeft()) {
//                pos.x -= speed;
                    ac.x = -LATERAL_ACCELERATION;
                    input.setAnimation(AbstractPlayerInputComponent.Animation.MOVEMENT);
                    input.setFacing(AbstractPlayerInputComponent.Direction.LEFT);
                } else if (input.isMovingRight()) {
//                pos.x += speed;
                    ac.x = LATERAL_ACCELERATION;
                    input.setAnimation(AbstractPlayerInputComponent.Animation.MOVEMENT);
                    input.setFacing(AbstractPlayerInputComponent.Direction.RIGHT);
                } else {
                    ac.x = 0;
                    input.setAnimation(AbstractPlayerInputComponent.Animation.IDLE);
                }

                if (input.isJumping() && body.isContactedWithFloor()) {
//                pos.x += speed;
                    if (warmth != null && !warmth.isFrozen()) {
                        ac.y = JUMPING_ACCELERATION * GameUtils.clamp(.7f, 1f, warmth.getWarmthFloat() * 2);
                        input.setAnimation(AbstractPlayerInputComponent.Animation.JUMP);
                    }
                }
            } else {
                ac.x = 0;
                input.setAnimation(AbstractPlayerInputComponent.Animation.IDLE);
            }

            if (input.isWalkRight()) {
                ac.x = LATERAL_ACCELERATION/6;
                input.setAnimation(AbstractPlayerInputComponent.Animation.MOVEMENT);
                input.setFacing(AbstractPlayerInputComponent.Direction.RIGHT);
            }


//            if (input.isMovingUp()) {
//                pos.y += speed;
//                input.setAnimation(AbstractPlayerInputComponent.Animation.MOVEMENT);
//                input.setFacing(AbstractPlayerInputComponent.Direction.UP);
//            }
//            if (input.isMovingDown()) {
//                pos.y -= speed;
//                input.setAnimation(AbstractPlayerInputComponent.Animation.MOVEMENT);
//                input.setFacing(AbstractPlayerInputComponent.Direction.DOWN);
//            }

//            playerAnimation.setCurrentAnimationId(getTextureKey(input.getAnimation(), input.getFacing()));
        }
    }

    private int getTextureKey(AbstractPlayerInputComponent.Animation animation, AbstractPlayerInputComponent.Direction direction) {
        return animation.ordinal() * AbstractPlayerInputComponent.Direction.values().length + direction.ordinal();
    }
}
