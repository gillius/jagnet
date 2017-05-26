package org.gillius.jagnet.examples;

import org.gillius.jagnet.*;
import org.gillius.jagnet.netty.NettyClient;
import org.gillius.jagnet.netty.NettyServer;
import org.gillius.jalleg.binding.ALLEGRO_COLOR;
import org.gillius.jalleg.binding.ALLEGRO_FONT;
import org.gillius.jalleg.binding.ALLEGRO_TRANSFORM;
import org.gillius.jalleg.framework.AllegroAddon;
import org.gillius.jalleg.framework.Direction;
import org.gillius.jalleg.framework.Game;
import org.gillius.jalleg.framework.audio.Beeper;
import org.gillius.jalleg.framework.math.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static java.util.Arrays.asList;
import static org.gillius.jalleg.binding.AllegroLibrary.*;

public class BallAndPaddleGame extends Game {
	private static final Logger log = LoggerFactory.getLogger(BallAndPaddleGame.class);

	private final Runnable network;
	private final boolean isServer;
	private final Connection connection;
	private final ObjectManager objectManager;

	private ALLEGRO_COLOR white;
	private ALLEGRO_FONT font;

	private ALLEGRO_TRANSFORM worldTransform;
	private ALLEGRO_TRANSFORM worldInverseTransform;

	private Rect leftPlayer;
	private Rect rightPlayer;

	private Rect board;

	private Random rnd = new Random();
	private MovingRect ball = new MovingRect();
	private Score score = new Score();

	public static class Score {
		int leftScore;
		int rightScore;
	}

	private Beeper beeper;

	public static class MovingRect {
		Rect pos;
		float dx;
		float dy;
	}

	public BallAndPaddleGame(ObjectManager objectManager, Runnable network, boolean isServer, Connection connection) {
		this.network = network;
		this.isServer = isServer;
		this.connection = connection;
		this.objectManager = objectManager;

		setAutoResize(true);
		setNewWindowTitle("Ball and Paddle " + (isServer ? "Server" : "Client"));

		ball.pos = new Rect(49.5f, 49.5f, 1, 1);
		leftPlayer = new Rect(4.5f, 45, 1, 10);
		rightPlayer = new Rect(94.5f, 45, 1, 10);
		objectManager.registerObject(ball).setOwned(isServer);
		objectManager.registerObject(leftPlayer).setOwned(isServer);
		objectManager.registerObject(rightPlayer).setOwned(!isServer);
		objectManager.registerObject(score).setOwned(isServer);
	}

	@Override
	protected void onAllegroStarted() {
		initAddons(AllegroAddon.Primitives, AllegroAddon.Font, AllegroAddon.Keyboard, AllegroAddon.Joystick,
		           AllegroAddon.Mouse, AllegroAddon.Audio);
		al_reserve_samples(1);

		white = al_map_rgb_f(1f, 1f, 1f);
		font = al_create_builtin_font();

		board = new Rect(0, 0, 100, 100);

		beeper = new Beeper();
		beeper.setGain(0.1f);

		worldTransform = new ALLEGRO_TRANSFORM();
		worldInverseTransform = new ALLEGRO_TRANSFORM();

		resetBall();
	}

	private void resetBall() {
		if (isServer) {
			ball.pos.x = 49.5f;
			ball.pos.y = 49.5f;
			ball.dx = 0.25f * (rnd.nextBoolean() ? 1f : -1f);
			ball.dy = 0.25f * (rnd.nextBoolean() ? 1f : -1f);
			objectManager.sendReliableUpdate(ball);
		}
	}

	@Override
	protected void update() {
		network.run();

		if (!connection.isOpen()) {
			stop();
			return;
		}

		float PADDLE_SPEED = 1f;
		boolean useMouse = isMouseButtonDown(0);
		Point mousePos = null;
		if (useMouse) {
			al_copy_transform(worldInverseTransform, worldTransform);
			al_invert_transform(worldInverseTransform);
			mousePos = getMousePosTransformed(worldInverseTransform);
		}

		if (isServer) {
			if (isKeyDown(ALLEGRO_KEY_A) || (useMouse && mousePos.x < 50 && mousePos.y < leftPlayer.centerY()))
				leftPlayer.move(0f, -PADDLE_SPEED);
			if (isKeyDown(ALLEGRO_KEY_Z) || (useMouse && mousePos.x < 50 && mousePos.y > leftPlayer.centerY()))
				leftPlayer.move(0f, PADDLE_SPEED);
			leftPlayer.constrainWithin(board);
			objectManager.sendReliableUpdate(leftPlayer);
		} else {
			if (isKeyDown(ALLEGRO_KEY_UP) || isJoyDirection(Direction.Up) || (useMouse && mousePos.x > 50 && mousePos.y < rightPlayer.centerY()))
				rightPlayer.move(0f, -PADDLE_SPEED);
			if (isKeyDown(ALLEGRO_KEY_DOWN) || isJoyDirection(Direction.Down) || (useMouse && mousePos.x > 50 && mousePos.y > rightPlayer.centerY()))
				rightPlayer.move(0f, PADDLE_SPEED);
			rightPlayer.constrainWithin(board);
			objectManager.sendReliableUpdate(rightPlayer);
		}

		ball.pos.move(ball.dx, ball.dy);

		if (ball.pos.collidesWith(rightPlayer)) {
			beeper.beep(200, gameTime + 0.1);
			ball.dx *= -1.2f;
			objectManager.sendReliableUpdate(ball); //only server will send these updates as owned is true only there
		} else if (ball.pos.collidesWith(leftPlayer)) {
			beeper.beep(150, gameTime + 0.1);
			ball.dx *= -1.2f;
			objectManager.sendReliableUpdate(ball); //only server will send these updates as owned is true only there
		}

		if (ball.pos.y <= board.y ||
		    ball.pos.bottom() >= board.bottom()) {
			ball.dy *= -1;
			beeper.beep(125, gameTime + 0.1);
		}

		if (isServer) {
			if (ball.pos.right() > board.right()) {
				score.leftScore++;
				objectManager.sendReliableUpdate(score);
				resetBall();
			}

			if (ball.pos.x < board.x) {
				score.rightScore++;
				objectManager.sendReliableUpdate(score);
				resetBall();
			}
		}

		beeper.update(gameTime);
	}

	@Override
	protected void render() {
		al_identity_transform(worldTransform);
		al_scale_transform(worldTransform, getDisplayWidth() / 100f, getDisplayHeight() / 100f);
		al_use_transform(worldTransform);

		draw(leftPlayer);
		draw(rightPlayer);
		draw(ball.pos);

		//Draw the line
		al_draw_line(50f, 0f, 50f, 100f, white, 1f);

		//Draw the scores
		al_draw_text(font, white, 25f, 0f, ALLEGRO_ALIGN_CENTRE, String.valueOf(score.leftScore));
		al_draw_text(font, white, 75f, 0f, ALLEGRO_ALIGN_CENTRE, String.valueOf(score.rightScore));
	}

	private void draw(Rect rect) {
		al_draw_filled_rectangle(rect.left(), rect.top(), rect.right(), rect.bottom(), white);
	}

	@Override
	protected void onStopped() {
		try {
			beeper.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws Exception {
		boolean isServer = false;
		ConnectionParams params = new ConnectionParams();
		try {
			isServer = "server".equalsIgnoreCase(args[0]);
			if (!isServer && !"client".equalsIgnoreCase(args[0]))
				printUsageAndExit();
			params.setByURI(args[1], isServer);
		} catch (Exception ignored) {
			printUsageAndExit();
		}

		Iterable<Class<?>> messages = asList(Rect.class, MovingRect.class, Score.class);
		ObjectManager objectManager = new ObjectManager();
		DeferredConnectionListener deferred = new DeferredConnectionListener(
				new ObjectManagerConnectionListener(objectManager)
		);
		params.registerMessages(messages);
		params.setListener(ConnectionListenerChain.of(new StandardConnectionListener(),
		                                              deferred));
		Connection connection = null;
		try {
			KryoCopier copier = new KryoCopier();
			copier.register(messages);
			objectManager.setOnUpdateListener(m -> copier.copy(m.getMessage(), m.getRegisteredObject()));


			if (isServer && !params.isProxyMode()) {
				FirstConnectionListener firstConnectionListener = new FirstConnectionListener();
				NettyServer server = new NettyServer();
				server.setAcceptPolicy(new AcceptFirstPolicy());
				server.setConnectionStateListener(firstConnectionListener);
				server.start(params);
				connection = firstConnectionListener.getConnection().get();
				server.stopAcceptingNewConnections();
				connection.getCloseFuture().thenRun(server::close);

			} else {
				log.info("Client connecting to {}", params.getRemoteAddress());
				NettyClient client = new NettyClient(params);
				client.start();
				connection = client.getConnection().get();
			}

			objectManager.setUpdateConnection(connection);
			new BallAndPaddleGame(objectManager, deferred, isServer, connection).run();

		} finally {
			if (connection != null)
				connection.close();
		}
	}

	private static void printUsageAndExit() {
		System.err.println("Arguments: server|client uri");
		System.err.println("Example: server tcp://0.0.0.0:56238");
		System.err.println("Example: server proxy+tcp://localhost:56238/?unique_tag");
		System.err.println("Example: client tcp://localhost:56238");
		System.err.println("Example: client proxy+tcp://localhost:56238/?unique_tag");
		System.exit(1);
	}
}
