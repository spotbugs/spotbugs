package ghIssues;

import java.util.ArrayList;
import java.util.List;

public class Issue1148 {
	private double TACKLE;
	private double PUNCH;
	private double KICK;
	private double HEADBUTT;
	private double MAGIC;
	
	public List<Integer> cpuData;
	public List<Integer> diskData;
	public List<Integer> memData;

	// False positive for SF_SWITCH_NO_DEFAULT
	public void setDamageParam(int i, double v){
		switch(i){	//this line is marked
			case 0:  TACKLE = v;
			case 1:  PUNCH = v;
			case 2:  KICK = v;
			case 3:  HEADBUTT = v;
			case 4:  MAGIC = v;
			default:
				break; // The compiler does not emit a GOTO for this so we can't tell if it was written, hence the SF_SWITCH_NO_DEFAULT false positive
		}
	}

	public void addData(UsageType _type, ArrayList<Integer> _list)
	{
		switch (_type)	//this line is marked
		{
		case CPU:
			this.cpuData.addAll(_list);

		case DISK:
			this.diskData.addAll(_list);

		case MEMORY:
			this.memData.addAll(_list);

		default:
			break;
		}
	}
	
	public enum UsageType {
		CPU, DISK, MEMORY
	}
}
