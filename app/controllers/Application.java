package controllers;

import play.*;
import play.mvc.*;
import play.libs.F.*;
import org.codehaus.jackson.*;
import views.html.*;
import models.*;

public class Application extends Controller {
	public static Result index() {
		return ok(index.render(request().remoteAddress()));
	}
	
	public static WebSocket<JsonNode> joinMainRoom(final String ip) {
		return new WebSocket<JsonNode>() {
			public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out){
				try { 
					MainRoom.join(ip, in, out);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
	}

}
