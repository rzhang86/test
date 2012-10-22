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

public class MainRoom extends UntypedActor {
	static ActorRef defaultRoom = Akka.system().actorOf(new Props(MainRoom.class));

	/*
    // Create a Robot, just for fun.
    static {
        new Robot(defaultRoom);
    }*/

	public static void join(final String ip, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) throws Exception {
		String result = (String) Await.result(ask(defaultRoom, new Join(ip, out), 1000), Duration.create(1, SECONDS));
		if(result.equals("OK")) {
			in.onMessage(new Callback<JsonNode>() {
				public void invoke(JsonNode event) {
					defaultRoom.tell(new Draw(ip, event.get("x").asText(), event.get("y").asText()));
				} 
			});
			
			in.onClose(new Callback0() {
				public void invoke() {
					defaultRoom.tell(new Quit(ip));
				}
			});
		}
		else {
			ObjectNode error = Json.newObject();
			error.put("error", result);
			out.write(error);
		}
	}

	
	Map<String, WebSocket.Out<JsonNode>> members = new HashMap<String, WebSocket.Out<JsonNode>>();
	public void onReceive(Object message) throws Exception {
		if(message instanceof Join) {
			Join join = (Join) message;
			members.put(join.ip, join.channel);
			getSender().tell("OK");
		}
		else if(message instanceof Draw)  {
			Draw Draw = (Draw) message;
			for(WebSocket.Out<JsonNode> channel : members.values()) {
				ObjectNode event = Json.newObject();
				event.put("ip", Draw.ip);
				event.put("x", Draw.x);
				event.put("y", Draw.y);

				ArrayNode m = event.putArray("members");
				for(String u : members.keySet()) {
					m.add(u);
				}

				channel.write(event);
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

	public static class Draw {
		final String ip;
		final String x, y;
		public Draw(String ip, String x, String y) {
			this.ip = ip;
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
