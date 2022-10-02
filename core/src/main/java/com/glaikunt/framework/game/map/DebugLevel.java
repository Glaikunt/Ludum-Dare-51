package com.glaikunt.framework.game.map;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.glaikunt.framework.application.ApplicationResources;
import com.glaikunt.framework.application.CommonActor;
import com.glaikunt.framework.cache.TiledCache;
import com.glaikunt.framework.game.enemy.EnemyActor;
import com.glaikunt.framework.game.enemy.Stance;
import com.glaikunt.framework.game.player.PlayerActor;

public class DebugLevel extends CommonActor implements Level {

    private final OrthogonalTiledMapRenderer renderer;
    private final TiledMapTileLayer background;

    private PlayerActor player;
    private final Array<EnemyActor> enemies = new Array<>();
    private final Array<HeatSourceActor> heatSources = new Array<>();
    private final Array<BreakableActor> breakables = new Array<>();

    public DebugLevel(ApplicationResources applicationResources, Stage front) {
        super(applicationResources);

        TiledMap map = applicationResources.getTiledMap(TiledCache.SOMETHING);
        this.renderer = new OrthogonalTiledMapRenderer(map);
        this.background = (TiledMapTileLayer) map.getLayers().get("Background");

        // Order here will also drive order actors are added to 'front' stage remember.

        createPlatforms(applicationResources, front, map);


        createCheckpoints(applicationResources, front, map);


        createIndoors(applicationResources, front, map);


        createHeatSources(applicationResources, front, map);


        createBreakables(applicationResources, front, map);


        createPlayer(applicationResources, front, map); // player before enemies
        createEnemies(applicationResources, front, map); // player before enemies
    }

    private void createPlayer(ApplicationResources applicationResources, Stage front, TiledMap map) {
        TiledMapTileLayer playerStart = (TiledMapTileLayer) map.getLayers().get("Player");
        for (int y = playerStart.getHeight(); y >= 0; y--) {
            float yPos = (y * playerStart.getTileHeight());
            for (int x = 0; x < playerStart.getWidth(); x++) {
                float xPos = (x * playerStart.getTileWidth());

                TiledMapTileLayer.Cell playerStartCell = playerStart.getCell(x, y);
                if (playerStartCell != null) {

                    if (player != null) {
                        throw new IllegalStateException("Player already set");
                    } else {
                        this.player = new PlayerActor(applicationResources, new Vector2(xPos, yPos));
                        front.addActor(player);
                    }
                }
            }
        }
    }

    private static void createIndoors(ApplicationResources applicationResources, Stage front, TiledMap map) {
        MapLayer indoorAreas = map.getLayers().get("Inside");
        for (MapObject mapObject : indoorAreas.getObjects()) {

            if (mapObject instanceof RectangleMapObject) {
                RectangleMapObject r = (RectangleMapObject) mapObject;
                float x = r.getRectangle().getX();
                float y = r.getRectangle().getY();
                Vector2 pos = new Vector2(x, y);

                float width = r.getRectangle().getWidth();
                float height = r.getRectangle().getHeight();
                Vector2 size = new Vector2(width, height);

                front.addActor(new IndoorAreaActor(applicationResources, pos, size));
            }
        }
    }

    private static void createCheckpoints(ApplicationResources applicationResources, Stage front, TiledMap map) {
        MapLayer levelCheckpoint = map.getLayers().get("Checkpoint");
        for (MapObject mapObject : levelCheckpoint.getObjects()) {

            if (mapObject instanceof RectangleMapObject) {
                RectangleMapObject r = (RectangleMapObject) mapObject;
                float x = r.getRectangle().getX();
                float y = r.getRectangle().getY();
                Vector2 pos = new Vector2(x, y);

                float width = r.getRectangle().getWidth();
                float height = r.getRectangle().getHeight();
                Vector2 size = new Vector2(width, height);

                front.addActor(new CheckPointActor(applicationResources, pos, size));
            }
        }
    }

    private static void createPlatforms(ApplicationResources applicationResources, Stage front, TiledMap map) {
        MapLayer levelCollision = map.getLayers().get("Platforms");
        for (MapObject mapObject : levelCollision.getObjects()) {

            if (mapObject instanceof RectangleMapObject) {
                RectangleMapObject r = (RectangleMapObject) mapObject;
                float x = r.getRectangle().getX();
                float y = r.getRectangle().getY();
                Vector2 pos = new Vector2(x, y);

                float width = r.getRectangle().getWidth();
                float height = r.getRectangle().getHeight();
                Vector2 size = new Vector2(width, height);

                front.addActor(new BlockActor(applicationResources, pos, size));
            }
        }
    }

    private void createBreakables(ApplicationResources applicationResources, Stage front, TiledMap map) {
        TiledMapTileLayer items = (TiledMapTileLayer) map.getLayers().get("Breakable");
        for (int y = items.getHeight(); y >= 0; y--) {
            float yPos = (y * items.getTileHeight());
            for (int x = 0; x < items.getWidth(); x++) {
                float xPos = (x * items.getTileWidth());

                TiledMapTileLayer.Cell startCell = items.getCell(x, y);
                if (startCell != null) {
                    BreakableActor breakable = new BreakableActor(applicationResources, new Vector2(xPos, yPos));
                    breakables.add(breakable);
                    front.addActor(breakable);
                }
            }
        }
    }

    private void createHeatSources(ApplicationResources applicationResources, Stage front, TiledMap map) {
        TiledMapTileLayer heatsources = (TiledMapTileLayer) map.getLayers().get("Heatsource");
        for (int y = heatsources.getHeight(); y >= 0; y--) {
            float yPos = (y * heatsources.getTileHeight());
            for (int x = 0; x < heatsources.getWidth(); x++) {
                float xPos = (x * heatsources.getTileWidth());

                TiledMapTileLayer.Cell startCell = heatsources.getCell(x, y);
                if (startCell != null) {
                    HeatSourceActor heatsource = new HeatSourceActor(applicationResources, new Vector2(xPos, yPos));
                    heatSources.add(heatsource);
                    front.addActor(heatsource);
                }
            }
        }
    }

    private void createEnemies(ApplicationResources applicationResources, Stage front, TiledMap map) {
            TiledMapTileLayer enemySpawns = (TiledMapTileLayer) map.getLayers().get("EnemySpawn");
            for (int y = enemySpawns.getHeight(); y >= 0; y--) {
                float yPos = (y * enemySpawns.getTileHeight());
                for (int x = 0; x < enemySpawns.getWidth(); x++) {
                    float xPos = (x * enemySpawns.getTileWidth());

                    TiledMapTileLayer.Cell startCell = enemySpawns.getCell(x, y);
                    if (startCell != null) {
                        EnemyActor enemy = new EnemyActor(applicationResources, new Vector2(xPos, yPos), this, Stance.AGGRESSIVE);
                        enemies.add(enemy);
                        front.addActor(enemy);
                    }
                }
            }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

        renderer.getBatch().begin();
        renderer.renderTileLayer(background);
        renderer.getBatch().end();
    }

    @Override
    public void act(float delta) {

        if (getStage() != null) {
            renderer.setView((OrthographicCamera) getStage().getCamera());
        }
    }

    public PlayerActor getPlayer() {
        return player;
    }

    public Array<HeatSourceActor> getHeatSources() {
        return heatSources;
    }

    public Array<BreakableActor> getBreakables() {
        return breakables;
    }

    public void removeBreakable(Entity entity) {
        BreakableActor match = null;
        for (BreakableActor b : breakables) {
            if (b.getEntity() == entity) {
                match = b;
                break;
            }
        }
        if (match != null) {
            match.remove();
            breakables.removeValue(match, true);
        }
    }
}
