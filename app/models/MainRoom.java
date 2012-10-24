package models;

import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;
import akka.dispatch.*;
import static akka.pattern.Patterns.ask;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;

import java.util.*;

import static java.util.concurrent.TimeUnit.*;

//import controllers.Application;
import models.Stroke;

public class MainRoom extends UntypedActor {
	static ActorRef defaultRoom = Akka.system().actorOf(new Props(MainRoom.class));

	/*
    // Create a Robot, just for fun.
    static {
        new Robot(defaultRoom);
    }*/

	public static void join(final String ip, final WebSocket.In<JsonNode> in, final WebSocket.Out<JsonNode> out) throws Exception {
		String result = (String) Await.result(ask(defaultRoom, new Join(ip, out), 1000), Duration.create(1, SECONDS));
		if(result.equals("OK")) {
			in.onMessage(new Callback<JsonNode>() {
				public void invoke(JsonNode event) {
					try {
						attemptDraw(ip, event.get("name").asText(), event.get("x").asText(), event.get("y").asText(), out);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				} 
			});
			
			in.onClose(new Callback0() {
				public void invoke() {
					defaultRoom.tell(new Quit(ip));
				}
			});
		}
		else {
			ObjectNode jsonNode = Json.newObject();
			jsonNode.put("error", result);
			out.write(jsonNode);
		}
	}
	
	public static void attemptDraw(final String ip, final String name, final String x, final String y, final WebSocket.Out<JsonNode> out) throws Exception {
		long timeTilReady = (long) Await.result(ask(defaultRoom, new AttemptDraw(ip, name, x, y), 1000), Duration.create(1, SECONDS));
		if(timeTilReady < 0) {
			defaultRoom.tell(new Draw(name, x, y));
			ObjectNode jsonNode = Json.newObject();
			jsonNode.put("newStrokeTime", System.currentTimeMillis());
			out.write(jsonNode);
		}
		else {
			ObjectNode jsonNode = Json.newObject();
			jsonNode.put("timeTilReady", timeTilReady);
			out.write(jsonNode);
		}
	}

	
	Map<String, WebSocket.Out<JsonNode>> members = new HashMap<String, WebSocket.Out<JsonNode>>();
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			Join join = (Join) message;
			members.put(join.ip, join.channel);
			Stroke[] initialStrokes = Stroke.getStrokes();
			
			int sendSize = 100;
			String[] names = new String[sendSize];
			int[] xs = new int[sendSize];
			int[] ys = new int[sendSize];
			for(int i = 0; i < initialStrokes.length; i++) {
				int slot = i % sendSize;
				names[slot] = initialStrokes[i].name;
				xs[slot] = initialStrokes[i].x;
				ys[slot] = initialStrokes[i].y;
				if(slot == names.length - 1) {
					ObjectNode jsonNode = Json.newObject();
					jsonNode.put("names", Json.toJson(names));
					jsonNode.put("xs", Json.toJson(xs));
					jsonNode.put("ys", Json.toJson(ys));
					join.channel.write(jsonNode);
				}
			}
			sendSize = initialStrokes.length % sendSize;
			if (sendSize > 0) {
				ObjectNode jsonNode = Json.newObject();
				jsonNode.put("names", Json.toJson(Arrays.copyOf(names, sendSize)));
				jsonNode.put("xs", Json.toJson(Arrays.copyOf(xs, sendSize)));
				jsonNode.put("ys", Json.toJson(Arrays.copyOf(ys, sendSize)));
				join.channel.write(jsonNode);
			}
			getSender().tell("OK");
		}
		else if(message instanceof AttemptDraw)  {
			AttemptDraw attemptDraw = (AttemptDraw) message;
			getSender().tell(Stroke.createStroke(attemptDraw.ip, attemptDraw.name, attemptDraw.x, attemptDraw.y));
		}
		else if(message instanceof Draw)  {
			Draw draw = (Draw) message;
			for(WebSocket.Out<JsonNode> channel : members.values()) {
				ObjectNode jsonNode = Json.newObject();
				jsonNode.put("name", draw.name);
				jsonNode.put("x", draw.x);
				jsonNode.put("y", draw.y);
				ArrayNode m = jsonNode.putArray("members");
				for(String u : members.keySet()) {
					m.add(u);
				}
				channel.write(jsonNode);
			}
		}
		else if(message instanceof Quit)  {
			Quit quit = (Quit) message;
			members.remove(quit.ip);
		}
		else {
			unhandled(message);
		}
	}

	
	public static class Join {
		final String ip;
		final WebSocket.Out<JsonNode> channel;
		public Join(String ip, WebSocket.Out<JsonNode> channel) {
			this.ip = ip;
			this.channel = channel;
		}
	}
	
	public static class AttemptDraw {
		final String ip, name, x, y;
		public AttemptDraw(String ip, String name, String x, String y) {
			this.ip = ip;
			this.name = name;
			this.x = x;
			this.y = y;
		}
	}
	
	public static class Draw {
		final String name, x, y;
		public Draw(String name, String x, String y) {
			this.name = name;
			this.x = x;
			this.y = y;
		}
	}

	public static class Quit {
		final String ip;
		public Quit(String ip) {
			this.ip = ip;
		}
	}
}
