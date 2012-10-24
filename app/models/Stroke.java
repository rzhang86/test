package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

import com.avaje.ebean.*;

import controllers.Application;
import java.sql.Timestamp;

@Entity
public class Stroke extends Model {
	@Id
    public Long id;
	
	public String ip, name;
	public Timestamp date_time;
	public int x, y;
	
	public Stroke() {
		this.ip = "0:0:0:0:0:0:0:0";
		this.name = "anonymous";
		this.date_time = new Timestamp(System.currentTimeMillis()); 
		this.x = 0;
		this.y = 0;
	}
	
	public static Finder<Long,Stroke> find = new Finder<Long,Stroke>(Long.class, Stroke.class);
	
	public static long createStroke(String ip, String name, String x, String y) {
		Stroke lastStroke = find.where().eq("ip", ip).orderBy("date_time desc").setMaxRows(1).findUnique();
		long timeTilReady = -1;
		if(lastStroke != null) timeTilReady = Application.getCooldown() - (System.currentTimeMillis() - lastStroke.date_time.getTime());
		if(timeTilReady < 0) {
			Stroke newStroke = new Stroke();
			newStroke.ip = ip;
			newStroke.name = (name.length() > Application.getMaxNameLength() ? name.substring(0, Application.getMaxNameLength()) : name);
			newStroke.x = new Double(x).intValue();
			newStroke.y = new Double(y).intValue();
			newStroke.save();
		}
		return timeTilReady;
	}
	
	public static Stroke[] getStrokes() {
		return find.orderBy("date_time asc").findList().toArray(new Stroke[0]);
	}
	
	public static String getMostRecentName(String ip) {
		Stroke lastStroke = find.where().eq("ip", ip).orderBy("date_time desc").setMaxRows(1).findUnique();
		if(lastStroke != null) {
			return lastStroke.name;
		}
		else {
			return "anonymous";
		}
	}
	
	public static long getMostRecentTimestamp(String ip) {
		Stroke lastStroke = find.where().eq("ip", ip).orderBy("date_time desc").setMaxRows(1).findUnique();
		if(lastStroke != null) {
			return lastStroke.date_time.getTime();
		}
		else {
			return 0;
		}
	}
}