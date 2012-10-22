package models;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.F.*;

import akka.util.*;
import akka.actor.*;
import akka.dispatch.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;

import static java.util.concurrent.TimeUnit.*;

public class Robot {
	public Robot(ActorRef chatRoom) {
		WebSocket.Out<JsonNode> robotChannel = new WebSocket.Out<JsonNode>() {
			public void write(JsonNode frame) {
				Logger.of("robot").info(Json.stringify(frame));
			}
			
			public void close() {
			}
		};
		
		chatRoom.tell(new MainRoom.Join("Robot", robotChannel));
		
		Akka.system().scheduler().schedule(Duration.create(1000, SECONDS), Duration.create(1000, SECONDS), chatRoom, new MainRoom.Draw("Robot", "10", "10"));
	}
}