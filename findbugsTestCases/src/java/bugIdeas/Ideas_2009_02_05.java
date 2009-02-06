package bugIdeas;

import sun.util.calendar.BaseCalendar.Date;

public class Ideas_2009_02_05 {
	
	// Date.getYear returns years since 1900.
	
	public boolean isBefore2009(Date d) {
		return d.getYear() < 2009;
	}
	public boolean is2009(Date d) {
		return d.getYear() == 2009;
	}

}
